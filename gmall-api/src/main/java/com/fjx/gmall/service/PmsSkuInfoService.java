package com.fjx.gmall.service;

import com.fjx.gmall.bean.PmsSkuInfo;

public interface PmsSkuInfoService {

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuById(String skuId);
}
