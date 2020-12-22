package com.fjx.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.PmsProductSaleAttr;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.bean.PmsSkuSaleAttrValue;
import com.fjx.gmall.service.PmsSkuInfoService;
import com.fjx.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    PmsSkuInfoService skuService;

    @Reference
    SpuService spuService;

//    spuSaleAttrListCheckBySku
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap){
        //通过skuid获取skuinfo
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);
        List<PmsProductSaleAttr> pmsProductSaleAttrList = null;
        if(pmsSkuInfo!=null) {
            //获取商品销售属性列表
            pmsProductSaleAttrList = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(), skuId);
        }
        String skuSaleAttrJsonString = null;
        if(pmsSkuInfo!=null){
            //获取skuid对应商品的兄弟sku信息
            List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());
            //将兄弟sku的销售属性做为k，skuid做为v存入map中
            skuSaleAttrJsonString = skuService.getSkuSaleAttrHash(pmsSkuInfos);
        }

        modelMap.put("skuInfo",pmsSkuInfo);
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrList);
        modelMap.put("skuSaleAttrJsonString",skuSaleAttrJsonString);

        return "item";
    }

}


