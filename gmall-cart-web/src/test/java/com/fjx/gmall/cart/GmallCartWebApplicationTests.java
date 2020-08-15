package com.fjx.gmall.cart;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallCartWebApplicationTests {

    @Test
    public void contextLoads() {

        BigDecimal a = new BigDecimal("0");
        BigDecimal b = new BigDecimal("2");
        BigDecimal c = a.add(b);
        System.out.println(c);

    }

}
