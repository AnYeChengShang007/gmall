package com.fjx.gmall.service;

import com.fjx.gmall.bean.PmsBaseAttrInfo;
import com.fjx.gmall.bean.PmsBaseAttrValue;
import com.fjx.gmall.bean.PmsBaseSaleAttr;

import java.util.List;
import java.util.Set;

public interface AttrService {
    List<PmsBaseAttrInfo> pmsBaseAttrInfos(String catalog3Id);

    String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo);

    List<PmsBaseAttrValue> getAttrValueList(String attrId);

    List<PmsBaseSaleAttr> baseSaleAttrList();

    List<PmsBaseAttrInfo> getAttrValueListByIds(Set<String> set);
}
