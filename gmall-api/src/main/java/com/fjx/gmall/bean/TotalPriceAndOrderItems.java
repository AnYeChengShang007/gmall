package com.fjx.gmall.bean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author jinxing.feng
 * @version V1.0
 * @Package com.fjx.gmall.bean
 * @date 2021/2/15 2:28
 */

public class TotalPriceAndOrderItems implements Serializable {

    BigDecimal allPrice = new BigDecimal(0);

    List<OmsOrderItem> orderItems;

    public BigDecimal getAllPrice() {
        return allPrice;
    }

    public void setAllPrice(BigDecimal allPrice) {
        this.allPrice = allPrice;
    }

    public List<OmsOrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OmsOrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}
