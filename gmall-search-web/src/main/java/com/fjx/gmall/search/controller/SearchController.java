package com.fjx.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.bean.*;
import com.fjx.gmall.service.AttrService;
import com.fjx.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;


@Controller
@CrossOrigin
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;


    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    String index() {
        return "index";
    }

    @RequestMapping("list.html")
    String list(PmsSearchParam pmsSearchParam, ModelMap map) {
        String urlParam = getUrlParam(pmsSearchParam);
        // 调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchService.list(pmsSearchParam);
        // 抽取检索结果包含的平台属性id集合
        Set<String> set = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
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
                tag:for (PmsBaseAttrValue pageShowPmsBaseAttrValue : pageShowPmsBaseAttrInfo.getAttrValueList()) {
                    String pageShowPmsBaseAttrValueId = pageShowPmsBaseAttrValue.getId();
                    for (PmsSkuAttrValue searchParamAttrValue : searchParamAttrValueList) {
                        if (searchParamAttrValue.getValueId().equals(pageShowPmsBaseAttrValueId)) {
                            //如果attrValueList不为空，说明当前请求中包含属性的参数，每一个属性参数，都会生成一个面包屑
                            PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                            pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, searchParamAttrValue.getValueId()));
                            pmsSearchCrumb.setValueId(searchParamAttrValue.getValueId());
                            //面包屑的属性值名称就是要删除的商品属性名称
                            pmsSearchCrumb.setValueName(pageShowPmsBaseAttrInfo.getAttrName()+":"+pageShowPmsBaseAttrValue.getValueName());
                            crumbs.add(pmsSearchCrumb);
                            iterator.remove();
                            break tag;
                        }
                    }
                }
            }
        }

        map.put("skuLsInfoList", pmsSearchSkuInfoList);
        map.put("urlParam", urlParam);
        map.put("attrValueSelectedList", crumbs);
        map.put("attrList", pageShowPmsBaseAttrInfos);
        map.put("keyword", pmsSearchParam.getKeyword());
        return "list";
    }

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
            urlParam = urlParam + "&";
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            urlParam = urlParam + "&";
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
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
