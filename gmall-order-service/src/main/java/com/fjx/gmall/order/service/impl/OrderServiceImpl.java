package com.fjx.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.fjx.gmall.bean.OmsOrder;
import com.fjx.gmall.order.mapper.OmsOrderItemMapper;
import com.fjx.gmall.order.mapper.OmsOrderMapper;
import com.fjx.gmall.service.OrderService;
import com.fjx.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper orderMapper;

    @Autowired
    OmsOrderItemMapper orderItemMapper;

    @Reference
    OrderService orderService;

    @Override
    public String getTradeCode(String memberId) {
        Jedis jedis = null;
        String tradeCode = null;
        try{
            jedis = redisUtil.getJedis();
            if(jedis!=null){
                String tradeKey = "user:"+memberId+"tradeCode";
                tradeCode = UUID.randomUUID().toString();
                jedis.setex(tradeKey,60*15,tradeCode);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String memberId,String tradeCode) {
        Jedis jedis = null;
        try{
            jedis = redisUtil.getJedis();
            if(jedis!=null){
                String tradeKey = "user:"+memberId+"tradeCode";
                //可以使用lua脚本发现key同时删除，防止并发订单攻击
                String tradeCodeFromCache = jedis.get(tradeKey);
                if(tradeCodeFromCache!=null && tradeCodeFromCache.equals(tradeCode)){
                    if(jedis.del(tradeKey)>0){
                        return true;
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }
        return false;
    }

    @Override
    public void saveOrder(OmsOrder order) {
        //保存订单表
        orderMapper.insertSelective(order);
        //保存订单详情
        final String orderId = order.getId();
        order.getOrderItems().forEach(orderItem->{
            orderItem.setOrderId(orderId);
            orderItemMapper.insertSelective(orderItem);
            //删除购物车
            //---
//            orderService.delCart();
        });
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder order = new OmsOrder();
        order.setOrderSn(outTradeNo);
        OmsOrder res = orderMapper.selectOne(order);
        return res;
    }


}
