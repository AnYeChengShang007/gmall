package com.fjx.gmall.service;

import com.fjx.gmall.bean.OmsCartItem;
import org.springframework.ui.ModelMap;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    OmsCartItem findCarItemByUser(String memberId, String skuId);

    void addCart(OmsCartItem omsCartItem);

    void updateCart(OmsCartItem insertCartItem);

    void flushCache(String memberId);

    List<OmsCartItem> cartList(String memberId);

    void checkCart(OmsCartItem omsCartItem);

    BigDecimal getTotalPrice(List<OmsCartItem> omsCartItems);

    Integer getTotalNumber(String memberId);

    void delCart(String productSkuId);
}
