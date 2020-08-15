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
import com.fjx.gmall.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PmsSkuInfoServiceImpl implements PmsSkuInfoService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

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
        pmsSkuInfo.setProductId(pmsSkuInfo.getSpuId());
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

    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        PmsSkuInfo res = null;
        Jedis jedis = null;
        //连接缓存
        jedis = redisUtil.getJedis();
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
                log.error("访问skuInfo错误");
            } finally {
                lock.unlock();
            }
           /* String ok = null;
            //标识设置是否成功
            boolean tag = false;
            String lockStringValue = UUID.randomUUID().toString();
            ok = jedis.set(key + ":lock", lockStringValue, "nx", "ex", 10);
            tag = StringUtils.isNotBlank(ok) && ok.equals("OK");
            if (!tag) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuById(skuId);
            }*/
           /* //释放锁
            String unlockStringValue = jedis.get(key + ":lock");
            if (StringUtils.isNotBlank(unlockStringValue) && unlockStringValue.equals(lockStringValue))
                jedis.del(key + ":lock");*/
        }
        return res;
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
}
