package com.fjx.gmall.cart.mapper;

import com.fjx.gmall.bean.OmsCartItem;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

public interface OmsCartItemMapper extends Mapper<OmsCartItem> {

    int insertCartItem(OmsCartItem omsCartItem);

    OmsCartItem cartExistByUser(@Param("memberId") String memberId,@Param("skuId") String skuId);

    int updateCart(OmsCartItem omsCartItem);

}
