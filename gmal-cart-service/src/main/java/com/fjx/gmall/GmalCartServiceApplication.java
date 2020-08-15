package com.fjx.gmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.fjx.gmall.cart.mapper")
public class GmalCartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmalCartServiceApplication.class, args);
    }

}
