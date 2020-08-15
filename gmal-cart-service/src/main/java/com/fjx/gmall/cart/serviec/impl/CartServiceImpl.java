package com.fjx.gmall.cart.serviec.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.OmsCartItem;
import com.fjx.gmall.cart.mapper.OmsCartItemMapper;
import com.fjx.gmall.service.CartService;
import com.fjx.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public OmsCartItem cartExistByUser(String memberId, String skuId) {
        OmsCartItem res = omsCartItemMapper.cartExistByUser(memberId,skuId);
        return res;
    }


    @Override
    public void addCart(OmsCartItem omsCartItem) {

        if (StringUtils.isNotBlank(omsCartItem.getMemberId()))
            omsCartItemMapper.insertSelective(omsCartItem);

    }

    @Override
    public void updateCart(OmsCartItem insertCartItem) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id",insertCartItem.getId());
        omsCartItemMapper.updateByExampleSelective(insertCartItem,example);
    }

    @Override
    public void flushCache(String memberId) {

        //从数据库中查询该用户的购物车，将购物车同步到缓存中
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(memberId);
        List<OmsCartItem> oldCartItems = omsCartItemMapper.select(cartItem);
        //同步缓存,因为以后要选择更新的商品为了不使一次新拿取所有数据，选择hash结构来存储。
        Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>();
        for (OmsCartItem oldCartItem : oldCartItems) {
            map.put(oldCartItem.getProductSkuId(), JSON.toJSONString(oldCartItem));
        }
        jedis.del("user:"+memberId+":cart");
        jedis.hmset("user:"+memberId+":cart",map);

    }

    @Override
    public List<OmsCartItem> cartList(String memberId) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();
        List<String> list = jedis.hvals("user:" + memberId + ":cart");
        for (String s : list) {
            OmsCartItem cartItem = JSON.parseObject(s, OmsCartItem.class);
            omsCartItems.add(cartItem);
        }
        return omsCartItems;
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem) {
        Example example = new Example(omsCartItem.getClass());
        example.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId()).
                andEqualTo("productSkuId",omsCartItem.getProductSkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem,example);
        flushCache(omsCartItem.getMemberId());
    }
}
