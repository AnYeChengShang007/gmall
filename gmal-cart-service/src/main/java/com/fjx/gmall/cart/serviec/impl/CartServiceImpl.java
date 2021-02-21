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

import java.math.BigDecimal;
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
    public OmsCartItem findCarItemByUser(String memberId, String skuId) {
        OmsCartItem res = omsCartItemMapper.findCarItemByUser(memberId, skuId);
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
        example.createCriteria().andEqualTo("id", insertCartItem.getId());
        omsCartItemMapper.updateByExampleSelective(insertCartItem, example);
    }

    @Override
    public void flushCache(String memberId) {

        //从数据库中查询该用户的购物车，将购物车同步到缓存中
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(memberId);
        List<OmsCartItem> cartItems = omsCartItemMapper.select(cartItem);
        //同步缓存,因为以后要选择更新的商品为了不使一次新拿取所有数据，选择hash结构来存储。
        Jedis jedis = redisUtil.getJedis();
        Map<String, String> map = new HashMap<>();
        for (OmsCartItem item : cartItems) {
            BigDecimal price = item.getPrice();
            Integer quantity = item.getQuantity();
            BigDecimal totalPrice = price.multiply(new BigDecimal(quantity));
            item.setTotalPrice(totalPrice);
            map.put(item.getProductSkuId(), JSON.toJSONString(item));
        }
        jedis.del("user:" + memberId + ":cart");
        if (map.size() > 0) {
            jedis.hmset("user:" + memberId + ":cart", map);
        } else {
            jedis.set("user:" + memberId + ":cart", "");
        }

    }

    @Override
    public List<OmsCartItem> cartList(String memberId) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            List<String> list = jedis.hvals("user:" + memberId + ":cart");
            if (list != null && list.size() > 0) {
                for (String s : list) {
                    OmsCartItem cartItem = JSON.parseObject(s, OmsCartItem.class);
                    omsCartItems.add(cartItem);
                }
            } else {
                Example example = new Example(OmsCartItem.class);
                example.createCriteria().andEqualTo("memberId", memberId);
                omsCartItems = omsCartItemMapper.selectByExample(example);
                for (OmsCartItem item : omsCartItems) {
                    BigDecimal price = item.getPrice();
                    Integer quantity = item.getQuantity();
                    BigDecimal totalPrice = price.multiply(new BigDecimal(quantity));
                    item.setTotalPrice(totalPrice);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return omsCartItems;
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }

        return omsCartItems;
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem) {
        Example example = new Example(omsCartItem.getClass());
        example.createCriteria().andEqualTo("memberId", omsCartItem.getMemberId()).
                andEqualTo("productSkuId", omsCartItem.getProductSkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem, example);
        flushCache(omsCartItem.getMemberId());
    }

    @Override
    public BigDecimal getTotalPrice(List<OmsCartItem> omsCartItems) {
        BigDecimal allPrice = new BigDecimal("0");
        for (OmsCartItem cartItem : omsCartItems) {
            Integer quantity = cartItem.getQuantity();
            BigDecimal price = cartItem.getPrice();
            BigDecimal totalPrice = price.multiply(new BigDecimal(quantity));
            cartItem.setTotalPrice(totalPrice);
            if (cartItem.getIsChecked().equals("1"))
                allPrice = allPrice.add(totalPrice);
        }
        return allPrice;
    }

    @Override
    public Integer getTotalNumber(String memberId) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("memberId", memberId);
        int num = omsCartItemMapper.selectCountByExample(example);
        return num;
    }

    @Override
    public void delCart(String productSkuId) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("productSkuId", productSkuId);
        omsCartItemMapper.deleteByExample(example);
    }
}
