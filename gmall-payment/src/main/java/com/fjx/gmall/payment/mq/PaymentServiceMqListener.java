package com.fjx.gmall.payment.mq;

import com.fjx.gmall.bean.PaymentInfo;
import com.fjx.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

/**
 * @author 冯金星
 * @date 2020/9/1/0001 上午 10:44
 */

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no = mapMessage.getString("out_trade_no");
        //队列是持久化的，新的客户端会自动消费
        Integer count = 0;
        String number = mapMessage.getString("count");
        if(number!=null)
            count = Integer.getInteger(number);
        //调用支付宝检查接口
        Map<String, Object> result = paymentService.checkAlipayPayment(out_trade_no);
        if (result != null) {
            String trade_status = (String) result.get("trade_status");
            //根据查询的支付状态结果，判断是否进行下一次的延迟任务还是支付成功更新数据和后续任务
            if (trade_status.equals("TRADE_SUCCESS")) {

                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn(out_trade_no);
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setAlipayTradeNo((String) result.get("trade_no"));// 支付宝的交易凭证号
                paymentInfo.setCallbackContent((String) result.get("call_back_content"));//回调请求字符串
                paymentInfo.setCallbackTime(new Date());
                // 更新用户的支付状态
                paymentService.updatePayment(paymentInfo);
                //支付成功后，引起的系统服务-》订单服务更新-》库存服务-》物流
                return;
            }
        }

        //继续发送延迟检查任务，计算延迟时间
        count--;
        if(count>0)
            paymentService.sendDelayPaymentResultCheckQueue(out_trade_no, count);
    }
}
