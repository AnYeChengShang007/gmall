package com.fjx.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.*;
import com.fjx.gmall.manage.config.FTPConfig;
import com.fjx.gmall.manage.utils.FtpUtil;
import com.fjx.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@CrossOrigin
public class SpuController {

    static SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");

    @Autowired
    FTPConfig ftpConfig;

    @Reference
    SpuService spuService;

    //    http://127.0.0.1:8081/spuList?catalog3Id=61
    @RequestMapping("spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(@RequestParam String catalog3Id) {
        List<PmsProductInfo> PmsProductInfos = spuService.spuList(catalog3Id);
        return PmsProductInfos;
    }

    //    http://127.0.0.1:8081/saveSpuInfo
    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo) {
        spuService.saveSpuInfo(pmsProductInfo);
        return "success";
    }

    //    http://127.0.0.1:8081/fileUpload
    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam MultipartFile file) {
        String url = null;
        try {
            String oldName = file.getOriginalFilename();
            String newName = UUID.randomUUID().toString().replace("-", "")
                    + oldName.substring(oldName.lastIndexOf("."));
            String year = format.format(new Date());
            boolean result = FtpUtil.uploadFile(ftpConfig.getHost(), ftpConfig.getPort(), ftpConfig.getUsername(), ftpConfig.getPassword(),
                    ftpConfig.getVbasePath(), year, newName, file.getInputStream());
            if (result) {
                url = ftpConfig.getVideo_base_path() + "/" + year + "/" + newName;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

    //    http://127.0.0.1:8081/spuSaleAttrList?spuId=24
    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<PmsProductSaleAttr> spuSaleAttrList(@RequestParam String spuId) {
        List<PmsProductSaleAttr> pmsProductSaleAttrList = spuService.spuSaleAttrList(spuId);
        return pmsProductSaleAttrList;
    }

    //    http://127.0.0.1:8081/spuImageList?spuId=24
    @RequestMapping("spuImageList")
    @ResponseBody
    public List<PmsProductImage> spuImageList(@RequestParam String spuId) {
        List<PmsProductImage> pmsProductImageList = spuService.spuImageList(spuId);
        return pmsProductImageList;
    }
}
