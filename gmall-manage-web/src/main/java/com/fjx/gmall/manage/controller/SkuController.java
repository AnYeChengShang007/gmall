package com.fjx.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.PmsSkuImage;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.PmsSkuInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@CrossOrigin
@Controller
public class SkuController {

    @Reference
    PmsSkuInfoService pmsSkuInfoService;

    //    http://127.0.0.1:8081/saveSkuInfo
    @RequestMapping("saveSkuInfo")
    @ResponseBody
    public String saveSkuInfo(@RequestBody PmsSkuInfo pmsSkuInfo) {
        //检查此sku是否能进行插入



        pmsSkuInfo.setProductId(pmsSkuInfo.getSpuId());

        //处理默认图片
        if (StringUtils.isBlank(pmsSkuInfo.getSkuDefaultImg())) {
            List<PmsSkuImage> imgList = pmsSkuInfo.getSkuImageList();
            if (imgList.size() > 0) {
                pmsSkuInfo.setSkuDefaultImg(imgList.get(0).getImgUrl());
                imgList.get(0).setIsDefault(1 + "");
            }
        }
        pmsSkuInfoService.saveSkuInfo(pmsSkuInfo);
        return "success";
    }

}
