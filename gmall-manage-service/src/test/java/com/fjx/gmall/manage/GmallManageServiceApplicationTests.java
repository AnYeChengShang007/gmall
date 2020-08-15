package com.fjx.gmall.manage;

import com.fjx.gmall.util.RedisUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.jedis.Jedis;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageServiceApplicationTests {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    public void contextLoads() {
        Jedis jedis = redisUtil.getJedis();
        jedis.set("a","b");
        jedis.close();
    }

    @Test
    public void contextLoads2() {
        String a = redisTemplate.opsForValue().get("a");
        System.out.println(a);
    }

    @Test
    public void contextLoads3() {

        RLock lock = redissonClient.getLock("anyLock");



    }

}
