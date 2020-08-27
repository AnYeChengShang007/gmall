package com.fjx.gmall.service;

import com.fjx.gmall.bean.OmsOrder;

import java.math.BigDecimal;

public interface OrderService {
    String getTradeCode(String memberId);

    boolean checkTradeCode(String memberId,String tradeCode);

    void saveOrder(OmsOrder order);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);
}
