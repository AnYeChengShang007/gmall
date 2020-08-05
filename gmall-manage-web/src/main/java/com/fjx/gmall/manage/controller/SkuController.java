package com.fjx.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.PmsSkuInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@CrossOrigin
@Controller
public class SkuController {

    @Reference
    PmsSkuInfoService pmsSkuInfoService;

    //    http://127.0.0.1:8081/saveSkuInfo
    @RequestMapping("saveSkuInfo")
    @ResponseBody
    public String saveSkuInfo(@RequestBody PmsSkuInfo pmsSkuInfo) {
        pmsSkuInfoService.saveSkuInfo(pmsSkuInfo);
        return "success";
    }

}
