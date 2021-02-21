package com.fjx.gmall.service;

import com.fjx.gmall.bean.OmsCartItem;
import com.fjx.gmall.bean.OmsOrder;
import com.fjx.gmall.bean.OmsOrderItem;
import com.fjx.gmall.bean.TotalPriceAndOrderItems;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    String getTradeCode(String memberId);

    boolean checkTradeCode(String memberId,String tradeCode);

    void saveOrder(OmsOrder order);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);

    void updateOrder(OmsOrder omsOrder);

    TotalPriceAndOrderItems getTotalPrice(List<OmsCartItem> cartItems);

    List<OmsOrder> getOrderListByUserId(String memberId);
}
