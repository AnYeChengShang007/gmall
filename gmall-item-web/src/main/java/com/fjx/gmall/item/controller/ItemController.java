package com.fjx.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.bean.OmsCartItem;
import com.fjx.gmall.bean.PmsProductSaleAttr;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.CartService;
import com.fjx.gmall.service.PmsSkuInfoService;
import com.fjx.gmall.service.SpuService;
import com.fjx.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    CartService cartService;

    @Reference
    PmsSkuInfoService skuService;

    @Reference
    SpuService spuService;

    //    spuSaleAttrListCheckBySku
    @RequestMapping("{skuId}.html")
    @LoginRequired(loginSuccess = false)
    public String item(@PathVariable String skuId, ModelMap modelMap, HttpServletRequest request) {
        getCartItemQuantity(request, modelMap);
        //通过skuid获取skuinfo
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);
        List<PmsProductSaleAttr> pmsProductSaleAttrList = null;
        if (pmsSkuInfo != null) {
            //获取商品销售属性列表
            pmsProductSaleAttrList = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(), skuId);
        }
        String skuSaleAttrJsonString = null;
        if (pmsSkuInfo != null) {
            //获取skuid对应商品的兄弟sku信息
            List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());
            //将兄弟sku的销售属性做为k，skuid做为v存入map中
            skuSaleAttrJsonString = skuService.getSkuSaleAttrHash(pmsSkuInfos);
        }
        modelMap.put("skuInfo", pmsSkuInfo);
        modelMap.put("spuSaleAttrListCheckBySku", pmsProductSaleAttrList);
        modelMap.put("skuSaleAttrJsonString", skuSaleAttrJsonString);
        modelMap.put("originUrl",request.getRequestURL());
        return "item";
    }

    /**
     * 计算页面显示的购物车物品数量
     *
     * @param request
     * @param modelMap
     */
    public void getCartItemQuantity(HttpServletRequest request, ModelMap modelMap) {
        String memberId = (String) request.getAttribute("memberId");
        Integer cartItemQuantity = 0;
        if (org.apache.commons.lang3.StringUtils.isNotBlank(memberId)) {
            cartItemQuantity = cartService.getTotalNumber(memberId);
        } else {
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                cartItemQuantity = JSON.parseArray(cartListCookie, OmsCartItem.class).size();
            }
        }
        modelMap.put("cartItemQuantity", cartItemQuantity);
    }

}


