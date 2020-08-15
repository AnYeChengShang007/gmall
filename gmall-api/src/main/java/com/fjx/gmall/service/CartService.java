package com.fjx.gmall.service;

import com.fjx.gmall.bean.OmsCartItem;

import java.util.List;

public interface CartService {
    OmsCartItem cartExistByUser(String memberId, String skuId);

    void addCart(OmsCartItem omsCartItem);

    void updateCart(OmsCartItem insertCartItem);

    void flushCache(String memberId);

    List<OmsCartItem> cartList(String memberId);

    void checkCart(OmsCartItem omsCartItem);
}
