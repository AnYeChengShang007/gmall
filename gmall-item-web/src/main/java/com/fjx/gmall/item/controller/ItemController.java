package com.fjx.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.PmsProductSaleAttr;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.PmsSkuInfoService;
import com.fjx.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class ItemController {

    @Reference
    PmsSkuInfoService pmsSkuInfoService;

    @Reference
    SpuService spuService;

    @RequestMapping("index")
    public String index(){
        return "index";
    }

//    spuSaleAttrListCheckBySku
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap){
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoService.getSkuById(skuId);

        List<PmsProductSaleAttr> pmsProductSaleAttrList = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),skuId);
        modelMap.put("skuInfo",pmsSkuInfo);
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrList);
        return "item";
    }

}


