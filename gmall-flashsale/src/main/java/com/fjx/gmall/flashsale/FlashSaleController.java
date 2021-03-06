package com.fjx.gmall.flashsale;

import com.fjx.gmall.util.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * @author 冯金星
 * @date 2020/9/10/0010 下午 10:23
 */
@Controller
public class FlashSaleController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    /**
     * 先到先得式秒杀
     * @return
     */
    @RequestMapping("flashkill")
    @ResponseBody
    public String flashKill() {
        Jedis jedis = redisUtil.getJedis();
        RSemaphore semaphore = redissonClient.getSemaphore("106");
        boolean b = semaphore.tryAcquire();
        int num = Integer.parseInt(jedis.get("106"));
        if (b) {
            System.out.println("当前库存剩余" + num);
        }else {
            System.out.println("当前库存剩余" + num + ",失败");
        }
        jedis.close();
        return "1";
    }

    /**
     * 拼手气随机式秒杀
     * @return
     */
    @RequestMapping("kill")
    @ResponseBody
    public String kill() {
        String memberId = "1";
        Jedis jedis = redisUtil.getJedis();
        //开启商品的监控
        jedis.watch("106");
        int stock = Integer.parseInt(jedis.get("106"));
        if (stock > 0) {
            Transaction multi = jedis.multi();
            multi.incrBy("106", -1);
            List<Object> exec = multi.exec();
            if (exec != null && exec.size() > 0) {
                System.out.println("当前库存剩余" + stock);
                //发出订单消息
            } else {
                System.out.println("当前库存剩余" + stock + ",失败");
            }
        }
        jedis.close();
        return "1";
    }
}
