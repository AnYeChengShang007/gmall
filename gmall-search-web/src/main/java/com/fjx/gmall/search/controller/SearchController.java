package com.fjx.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.bean.*;
import com.fjx.gmall.service.AttrService;
import com.fjx.gmall.service.CartService;
import com.fjx.gmall.service.SearchService;
import com.fjx.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


@Controller
@CrossOrigin
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    CartService cartService;

    @Reference
    AttrService attrService;


    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    String index(HttpServletRequest request, ModelMap modelMap) {
        getCartItemQuantity(request, modelMap);
        return "index";
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
        if (StringUtils.isNotBlank(memberId)) {
            cartItemQuantity = cartService.getTotalNumber(memberId);
        } else {
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                cartItemQuantity = JSON.parseArray(cartListCookie, OmsCartItem.class).size();
            }
        }
        modelMap.put("cartItemQuantity", cartItemQuantity);
    }

    @RequestMapping("list.html")
    @LoginRequired(loginSuccess = false)
    String list(PmsSearchParam pmsSearchParam, ModelMap map, HttpServletRequest request) {
        getCartItemQuantity(request, map);
        String urlParam = getUrlParam(pmsSearchParam);
        // 调用搜索服务，返回搜索结果
        GmallSearchResult searchResult = searchService.list(pmsSearchParam, true);
        GmallSearchResult searchResult2 = searchService.list(pmsSearchParam, false);
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchResult.getSearchSkuInfos();
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList2 = searchResult2.getSearchSkuInfos();
        // 抽取检索结果包含的平台属性id集合
        Set<String> set = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList2) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String id = pmsSkuAttrValue.getValueId();
                set.add(id);
            }
        }
        // 根据valueId将属性列表查询出来
        List<PmsBaseAttrInfo> pageShowPmsBaseAttrInfos = attrService.getAttrValueListByIds(set);
        //面包屑
        List<PmsSearchCrumb> crumbs = null;
        // 对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组
        List<PmsSkuAttrValue> searchParamAttrValueList = pmsSearchParam.getSkuAttrValueList();
        if (searchParamAttrValueList != null) {
            //只有用户选择删选属性的时候才会产生面包屑
            crumbs = new ArrayList<>();
            Iterator<PmsBaseAttrInfo> iterator = pageShowPmsBaseAttrInfos.iterator();
            while (iterator.hasNext()) {
                PmsBaseAttrInfo pageShowPmsBaseAttrInfo = iterator.next();
                tag:
                for (PmsBaseAttrValue pageShowPmsBaseAttrValue : pageShowPmsBaseAttrInfo.getAttrValueList()) {
                    String pageShowPmsBaseAttrValueId = pageShowPmsBaseAttrValue.getId();
                    for (PmsSkuAttrValue searchParamAttrValue : searchParamAttrValueList) {
                        if (searchParamAttrValue.getValueId().equals(pageShowPmsBaseAttrValueId)) {
                            //如果attrValueList不为空，说明当前请求中包含属性的参数，每一个属性参数，都会生成一个面包屑
                            PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                            pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, searchParamAttrValue.getValueId()));
                            pmsSearchCrumb.setValueId(searchParamAttrValue.getValueId());
                            //面包屑的属性值名称就是要删除的商品属性名称
                            pmsSearchCrumb.setValueName(pageShowPmsBaseAttrInfo.getAttrName() + ":" + pageShowPmsBaseAttrValue.getValueName());
                            crumbs.add(pmsSearchCrumb);
                            iterator.remove();
                            break tag;
                        }
                    }
                }
            }
        }
        String queryStr = request.getQueryString();
        String preStr = null;
        String postStr = null;
        if (queryStr != null && queryStr.indexOf("page") >= 0) {
            String[] split = queryStr.split("&");
            String pagenum = "";
            for (String s : split) {
                if (s.contains("page")) {
                    pagenum = s.substring(5);
                    break;
                }
            }
            preStr = queryStr.substring(0, queryStr.indexOf("page") + 5);
            postStr = queryStr.substring(queryStr.indexOf("page") + 5 + pagenum.length());
        } else {
            preStr = request.getRequestURL() + "?page=";
        }
        String postStr1 = postStr;
        if (postStr == null) {
            postStr1 = "";
        }
        String upperUrl = preStr + (searchResult.getPageInfo().getCurrentPage() - 1) + postStr1;
        String downUrl = preStr + (searchResult.getPageInfo().getCurrentPage() + 1) + postStr1;
        if (!upperUrl.contains("http")) {
            upperUrl = request.getRequestURL() + "?" + upperUrl;
            downUrl = request.getRequestURL() + "?" + downUrl;
        }
        map.put("upperUrl", upperUrl);
        map.put("downUrl", downUrl);
        map.put("preStr", preStr);
        map.put("postStr", postStr);
        map.put("requestUrl", request.getRequestURL());
        map.put("queryString", request.getQueryString());
        map.put("pageInfo", searchResult.getPageInfo());
        map.put("skuLsInfoList", pmsSearchSkuInfoList);
        map.put("urlParam", urlParam);
        map.put("attrValueSelectedList", crumbs);
        map.put("attrList", pageShowPmsBaseAttrInfos);
        map.put("keyword", pmsSearchParam.getKeyword());
        return "list";
    }

    /**
     * 获取搜索参数
     *
     * @param pmsSearchParam
     * @return
     */
    private String getUrlParam(PmsSearchParam pmsSearchParam) {

        if (pmsSearchParam.getValueId() != null) {
            for (String s : pmsSearchParam.getValueId()) {
                if (pmsSearchParam.getSkuAttrValueList() == null) {
                    pmsSearchParam.setSkuAttrValueList(new ArrayList<>());
                }
                pmsSearchParam.getSkuAttrValueList().add(new PmsSkuAttrValue().setValueId(s));
            }
        }

        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        List<PmsSkuAttrValue> skuAttrValueList = pmsSearchParam.getSkuAttrValueList();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            urlParam = urlParam + "&keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            urlParam = urlParam + "&catalog3Id=" + catalog3Id;
        }

        if (skuAttrValueList != null) {

            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                urlParam = urlParam + "&valueId=" + pmsSkuAttrValue.getValueId();
            }
        }
        if (urlParam.indexOf("&") == 0) {
            urlParam = urlParam.substring(1);
        }
        return urlParam;

    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String delValueId) {

        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        List<PmsSkuAttrValue> skuAttrValueList = pmsSearchParam.getSkuAttrValueList();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            urlParam = urlParam + "&";
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            urlParam = urlParam + "&";
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }

        if (skuAttrValueList != null) {

            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                if (!delValueId.equals(pmsSkuAttrValue.getValueId())) {
                    urlParam = urlParam + "&valueId=" + pmsSkuAttrValue.getValueId();
                }
            }
        }
        if (urlParam.indexOf("&") == 0) {
            urlParam = urlParam.substring(1);
        }
        return urlParam;

    }


}
