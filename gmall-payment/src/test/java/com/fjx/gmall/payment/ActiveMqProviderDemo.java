package com.fjx.gmall.payment;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

/**
 * @author 冯金星
 * @date 2020/8/29/0029 上午 6:30
 */
public class ActiveMqProviderDemo {
    public static void main(String[] args) {

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Session session = null;
        Connection conn = null;
        try {
            conn = connectionFactory.createConnection();
            conn.start();
            //  是否开启事务
            session = conn.createSession(true, Session.SESSION_TRANSACTED);
            Queue q = session.createQueue("111");
            MessageProducer producer = session.createProducer(q);
            TextMessage message = new ActiveMQTextMessage();
            message.setText("我渴了，谁去倒杯水");
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
            session.commit();
        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            if(session!=null){
                try {
                    session.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
            if(conn!=null){
                try {
                    conn.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
