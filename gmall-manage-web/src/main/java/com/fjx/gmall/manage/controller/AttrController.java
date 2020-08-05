package com.fjx.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.PmsBaseAttrInfo;
import com.fjx.gmall.bean.PmsBaseAttrValue;
import com.fjx.gmall.bean.PmsBaseSaleAttr;
import com.fjx.gmall.service.AttrService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@CrossOrigin
public class AttrController {

    @Reference
    AttrService attrService;

    //    http://127.0.0.1:8081/attrInfoList?catalog3Id=61
    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<PmsBaseAttrInfo> pmsBaseAttrInfos(String catalog3Id) {
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.pmsBaseAttrInfos(catalog3Id);
        return pmsBaseAttrInfos;
    }

    //    http://127.0.0.1:8081/saveAttrInfo
    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(@RequestBody PmsBaseAttrInfo pmsBaseAttrInfo) {
        String success = attrService.saveAttrInfo(pmsBaseAttrInfo);
        return success;
    }

    //    http://127.0.0.1:8081/getAttrValueList?attrId=12
    @RequestMapping("getAttrValueList")
    @ResponseBody
    public List<PmsBaseAttrValue> getAttrValueList(@RequestParam String attrId) {
        List<PmsBaseAttrValue> attrValueList = attrService.getAttrValueList(attrId);
        return attrValueList;
    }

    //    http://127.0.0.1:8081/baseSaleAttrList
    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return attrService.baseSaleAttrList();
    }

}
