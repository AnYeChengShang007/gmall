package com.fjx.gmall.payment;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

/**
 * @author 冯金星
 * @date 2020/8/29/0029 上午 6:30
 */
public class ActiveMqConsumerDemo {
    public static void main(String[] args) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Session session = null;
        Connection conn = null;
        try {
            conn = connectionFactory.createConnection();
            conn.start();
            //  是否开启事务
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue q = session.createQueue("111");
            MessageConsumer consumer = session.createConsumer(q);
            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) message).getText();
                        System.out.println(text);

                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
