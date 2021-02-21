package com.fjx.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.PmsSkuAttrValue;
import com.fjx.gmall.bean.PmsSkuImage;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.bean.PmsSkuSaleAttrValue;
import com.fjx.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.fjx.gmall.manage.mapper.PmsSkuImageMapper;
import com.fjx.gmall.manage.mapper.PmsSkuInfoMapper;
import com.fjx.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.fjx.gmall.service.PmsSkuInfoService;
import com.fjx.gmall.util.ActiveMQUtil;
import com.fjx.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class PmsSkuInfoServiceImpl implements PmsSkuInfoService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        //插入skuinfo
        pmsSkuInfoMapper.insert(pmsSkuInfo);

        String skuId = pmsSkuInfo.getId();

        //插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        //插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }
        //插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
        //发布消息更新缓存同步
        //发布搜索引擎同步消息
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e1) {
            e1.printStackTrace();
        }
        try {
            //调用mq发送支付成功的消息
            Queue payment_success_queuqe= session.createQueue("SKU_ADD_QUEUQE");
            MessageProducer producer = session.createProducer(payment_success_queuqe);
            ActiveMQMapMessage message = new ActiveMQMapMessage();
            message.setString("sku_id", skuId);
            producer.send(message);
            session.commit();
        }catch (Exception e){
            //消息回滚
            if(session!=null){
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }
            }
        }finally {
            try {
                if(session!=null)
                    session.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
            try {
                if(connection!=null)
                    connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }

        }
    }

    public PmsSkuInfo getSkuFromDB(String skuId) {
        //查询商品对象
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo res = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //查询商品对应图片
        if (res != null) {
            PmsSkuImage pmsSkuImage = new PmsSkuImage();
            pmsSkuImage.setSkuId(skuId);
            List<PmsSkuImage> pmsSkuImageList = pmsSkuImageMapper.select(pmsSkuImage);
            res.setSkuImageList(pmsSkuImageList);
        }
        return res;
    }

    /**
     * 通过skuid获取sku信息，使用redission分布式锁框架
     * @param skuId
     * @return
     */
    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        PmsSkuInfo res = null;
        //连接缓存
        Jedis jedis = redisUtil.getJedis();
        //查询缓存
        String key = "sku:" + skuId + ":info";
        String skuInfoJsonString = jedis.get(key);
        if (StringUtils.isNotBlank(skuInfoJsonString)) {
            if (skuInfoJsonString.equals("null"))
                return res;
            res = JSON.parseObject(skuInfoJsonString, PmsSkuInfo.class);
        } else {
            //如果缓存没有查询数据库
            //设置分布式锁
            /**
             * 分布式锁的作用是防止redis系统奔溃之后mysql被多请求宕机造成的系统崩溃。
             * 原理 redis的setnx
             * set "key" "value" px 1000 nx
             */
            RLock lock = redissonClient.getLock("skuInfoLock");
            lock.lock();
            try {
                res = getSkuFromDB(skuId);
                if (res == null) {
                    //防止缓存击穿，设置缓存空对象
                    jedis.setex(key, 60 * 3, "null");
                } else {
                    //redis结果缓存redis
                    skuInfoJsonString = JSON.toJSONString(res);
                    jedis.set(key, skuInfoJsonString);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                jedis.close();
            }

        }
        return res;
    }

    /**
     * 通过skuid获取sku信息，使用lua脚本进行分布式锁
     * 用于redis失效后缓解mysql压力
     * @param skuId
     * @return
     */
    public PmsSkuInfo getSkuById1(String skuId) {
        PmsSkuInfo res = null;
        Jedis jedis = redisUtil.getJedis();
        String key = "sku:" + skuId + ":info";
        String skuInfoJsonString = jedis.get(key);
        if (StringUtils.isNotBlank(skuInfoJsonString)) {
            if (skuInfoJsonString.equals("null"))
                return res;
            res = JSON.parseObject(skuInfoJsonString, PmsSkuInfo.class);
        } else {
            String ok = null;
            //标识设置是否成功
            boolean tag = false;
            String token = UUID.randomUUID().toString();

            //问题1：当线程A访问超过此锁的过期值，那么在访问数据库后回来删除（释放）锁的时候可能会删除其他线程设置的锁。
            //解决1：锁的value可以设置为随机值，这样在回来删除的时候可以看是否是自己设置的锁。

            //问题2：当线程A访问数据库回来，判断锁是否是自己的，因为网络的原因，正好在执行删除锁的时候，锁过期了，此时删除的可能是别人的锁
            //解决2：使用Lua脚本

            //方案3：可以利用redis的自动过期机制，直接设置过期时间，其他的都不用管，这也是防止缓存击穿的方式

            ok = jedis.set(key + ":lock", token, "nx", "ex", 10);
            tag = StringUtils.isNotBlank(ok) && ok.equals("OK");
            if (!tag) {
                //设置失败，自旋（让该线程睡几秒，重新访问本方法）
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuById1(skuId);
            }
            //释放锁
            unLockByLua(jedis, key, token);
        }
        return res;
    }

    /**
     * 解锁分布式锁（不使用lua脚本）
     * @param jedis
     * @param key
     * @param token
     */
    void unLock(Jedis jedis, String key, String token) {
        String lockToken = jedis.get(key + ":lock");
        if (StringUtils.isNotBlank(lockToken) && lockToken.equals(token))
            jedis.del(key + ":lock");


    }

    /**
     * 解锁分布式锁（使用lua脚本）
     * @param jedis
     * @param key
     * @param token
     */
    void unLockByLua(Jedis jedis, String key, String token) {

        String script = "if redis.call('get',KEYS[1])==ARGV[1] " +
                "then return redis.call('del',KEYS[1]) " +
                "else return 0 end";
        jedis.eval(script, Collections.singletonList(key + ":lock"), Collections.singletonList(token));
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.getSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllPmsSkuInfos() {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        if (pmsSkuInfos != null) {
            for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
                PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
                pmsSkuAttrValue.setSkuId(pmsSkuInfo.getId());
                List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
                pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValueList);

            }
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String skuId, BigDecimal price) {
        boolean success = false;
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setPrice(price);
        skuInfo.setId(skuId);
        PmsSkuInfo res = pmsSkuInfoMapper.selectOne(skuInfo);
        if (res != null) {
            success = true;
        }
        return success;
    }

    /**
     * 由skuinfo获取属性：skuid的键值对的集合的json字符串
     *
     * @param pmsSkuInfos
     * @return
     */
    @Override
    public String getSkuSaleAttrHash(List<PmsSkuInfo> pmsSkuInfos) {
        String skuSaleAttrJsonString = "";
        String spuId = "";
        //将键值对从缓存中取出
        if (pmsSkuInfos != null && pmsSkuInfos.size() > 0) {
            spuId = pmsSkuInfos.get(0).getProductId();
            Jedis jedis = redisUtil.getJedis();
            try {
                skuSaleAttrJsonString = jedis.hget("skuSaleAttr", spuId);
                if (StringUtils.isNotBlank(skuSaleAttrJsonString)) {
                    return skuSaleAttrJsonString;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                jedis.close();
            }
        }


        Map<String, String> skuSaleAttrHash = new HashMap<>();
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k = "";
            String v = skuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                k += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";
            }
            skuSaleAttrHash.put(k, v);
        }

        skuSaleAttrJsonString = JSON.toJSONString(skuSaleAttrHash);
        //将键值对存入缓存
        if (pmsSkuInfos != null && pmsSkuInfos.size() > 0) {
            Jedis jedis = redisUtil.getJedis();
            try {
                jedis.hset("skuSaleAttr", pmsSkuInfos.get(0).getProductId(), skuSaleAttrJsonString);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                jedis.close();
            }

        }
        return skuSaleAttrJsonString;
    }
}
