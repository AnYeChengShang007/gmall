package com.fjx.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.OmsOrder;
import com.fjx.gmall.bean.OmsOrderItem;
import com.fjx.gmall.order.mapper.OmsOrderItemMapper;
import com.fjx.gmall.order.mapper.OmsOrderMapper;
import com.fjx.gmall.service.OrderService;
import com.fjx.gmall.util.ActiveMQUtil;
import com.fjx.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper orderMapper;

    @Autowired
    OmsOrderItemMapper orderItemMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

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

    @Override
    public void updateOrder(OmsOrder omsOrder) {
        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());

        OmsOrder omsOrderUpdate = new OmsOrder();

        omsOrderUpdate.setStatus(1);

        // 发送一个订单已支付的队列，提供给库存消费
        Connection connection = null;
        Session session = null;
        try{
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payhment_success_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payhment_success_queue);
            TextMessage textMessage=new ActiveMQTextMessage();//字符串文本
            //MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

            //查询订单对象，转化为json字符串，存入order_pay_queue的消息队列
            OmsOrder omsOrderParam = new OmsOrder();
            omsOrderParam.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrderReponse = orderMapper.selectOne(omsOrderParam);
            List<OmsOrderItem> select = orderItemMapper.select(new OmsOrderItem().setOrderSn(omsOrderParam.getOrderSn()));
            omsOrderReponse.setOrderItems(select);
            textMessage.setText(JSON.toJSONString(omsOrderReponse));
            orderMapper.updateByExampleSelective(omsOrderUpdate,e);
            producer.send(textMessage);
            session.commit();
        }catch (Exception ex){
            // 消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }
    }


}
