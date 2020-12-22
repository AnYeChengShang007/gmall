package com.fjx.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.fjx.gmall.bean.PaymentInfo;
import com.fjx.gmall.payment.mapper.PaymentInfoMaper;
import com.fjx.gmall.service.PaymentService;
import com.fjx.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {


    @Autowired
    PaymentInfoMaper paymentInfoMaper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMaper.insert(paymentInfo);
    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {

        //幂等性检查
        PaymentInfo queryForPaymentInfo = new PaymentInfo();
        queryForPaymentInfo.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo checkResult = paymentInfoMaper.selectOne(queryForPaymentInfo);
        String paymentStatus = checkResult.getPaymentStatus();
        if(StringUtils.isNotBlank(paymentStatus) && paymentStatus.equals("已支付")){
            return;
        }


        String orderSn = paymentInfo.getOrderSn();

        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn", orderSn);

        Connection connection = null;
        Session session = null;

        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e1) {
            e1.printStackTrace();
        }

        //支付成功后引发的系统服务-》订单服务的更新-》库存服务-》物流服务
        try {
            paymentInfoMaper.updateByExampleSelective(paymentInfo, example);
            //调用mq发送支付成功的消息
            Queue payment_success_queuqe= session.createQueue("PAYMENT_SUCCESS_QUEUQE");
            MessageProducer producer = session.createProducer(payment_success_queuqe);
            ActiveMQMapMessage message = new ActiveMQMapMessage();
            message.setString("out_trade_no", paymentInfo.getOrderSn());
            producer.send(message);
            session.commit();
        }catch (Exception e){
            //消息回滚
            if(session!=null){
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }
            }
        }finally {
            try {
                if(session!=null)
                    session.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
            try {
                if(connection!=null)
                    connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }

        }

    }

    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, int count) {
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        try{
            Queue payment_success_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);

            //TextMessage textMessage=new ActiveMQTextMessage();//字符串文本

            MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

            mapMessage.setString("out_trade_no",outTradeNo);

            // 为消息加入延迟时间--参数1 延迟方式
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*60);

            producer.send(mapMessage);

            session.commit();
        }catch (Exception ex){
            // 消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, Object> checkAlipayPayment(String out_trade_no) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,Object> queryMap = new HashMap<>();
        Map<String,Object> resultMap = null;
        queryMap.put("out_trade_no", out_trade_no);
        request.setBizContent(JSON.toJSONString(queryMap));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response!=null ){
            if(response.isSuccess()){
                resultMap = new HashMap<>();
                resultMap.put("out_trade_no", response.getOutTradeNo());
                resultMap.put("trade_no", response.getTradeNo());
                resultMap.put("trade_status", response.getTradeStatus());
                resultMap.put("call_back_content", response.getMsg());
            }else {

            }
        }
        return resultMap;
    }

}
