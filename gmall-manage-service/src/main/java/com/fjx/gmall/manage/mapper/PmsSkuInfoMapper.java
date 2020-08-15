package com.fjx.gmall.manage.mapper;

import com.fjx.gmall.bean.PmsSkuInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PmsSkuInfoMapper extends Mapper<PmsSkuInfo> {


    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);
}
