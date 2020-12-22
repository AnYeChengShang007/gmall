package com.fjx.gmall.service;

import com.fjx.gmall.bean.PmsSkuInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PmsSkuInfoService {

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuById(String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);

    List<PmsSkuInfo> getAllPmsSkuInfos();

    boolean checkPrice(String productSkuId, BigDecimal price);

    String getSkuSaleAttrHash(List<PmsSkuInfo> pmsSkuInfos);
}
