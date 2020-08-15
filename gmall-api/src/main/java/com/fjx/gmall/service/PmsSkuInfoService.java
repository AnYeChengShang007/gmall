package com.fjx.gmall.service;

import com.fjx.gmall.bean.PmsSkuInfo;

import java.util.List;

public interface PmsSkuInfoService {

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuById(String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);

    List<PmsSkuInfo> getAllPmsSkuInfos();
}
