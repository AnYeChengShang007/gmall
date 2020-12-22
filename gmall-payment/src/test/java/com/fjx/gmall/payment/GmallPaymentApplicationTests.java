package com.fjx.gmall.payment;

import com.fjx.gmall.util.ActiveMQUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallPaymentApplicationTests {

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Test
    public void contextLoads() throws JMSException {

        ConnectionFactory factory = activeMQUtil.getConnectionFactory();
        Connection connection = factory.createConnection();
        System.out.println(connection);

    }

}
