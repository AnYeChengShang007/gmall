package com.fjx.gmall.order.mq;

import com.fjx.gmall.bean.OmsOrder;
import com.fjx.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {

    @Autowired
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUQE", containerFactory = "jmsQueueListener")
    public void consumePaymentMsg(MapMessage mapMessage) throws JMSException {
        String out_trade_no = mapMessage.getString("out_trade_no");
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(out_trade_no);
        //设置订单状态为待发货
        omsOrder.setStatus(1);
        orderService.updateOrder(omsOrder);
    }
}
