package com.fjx.gmall.manage;

import com.fjx.gmall.manage.config.FTPConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GmallManageWebApplication.class)
public class GmallManageWebApplicationTests {

    @Autowired
    FTPConfig ftpConfig;


    @Test
    public void contextLoads() {
        System.out.println(ftpConfig);
    }

}
