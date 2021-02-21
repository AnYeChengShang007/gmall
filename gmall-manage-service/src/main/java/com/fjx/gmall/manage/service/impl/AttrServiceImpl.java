package com.fjx.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.fjx.gmall.bean.PmsBaseAttrInfo;
import com.fjx.gmall.bean.PmsBaseAttrValue;
import com.fjx.gmall.bean.PmsBaseSaleAttr;
import com.fjx.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.fjx.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.fjx.gmall.manage.mapper.PmsBaseSaleAttrMapper;
import com.fjx.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;

    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;

    @Override
    public List<PmsBaseAttrInfo> pmsBaseAttrInfos(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        //查询平台商品
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        for (PmsBaseAttrInfo baseAttrInfo : pmsBaseAttrInfos) {
            //查询平台商品属性值
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(baseAttrInfo.getId());
            List<PmsBaseAttrValue> pmsBaseAttrValueList = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            baseAttrInfo.setAttrValueList(pmsBaseAttrValueList);

        }
        return pmsBaseAttrInfos;
    }

    @Override
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        String id = pmsBaseAttrInfo.getId();
        List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
        if (StringUtils.isBlank(id)) {
            //保存平台属性
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
//            保存平台属性值
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }
        } else {
            //修改平台属性
            Example example = new Example(PmsBaseAttrInfo.class);
            example.createCriteria().andEqualTo("id", id);
            pmsBaseAttrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo, example);
            //修改平台属性值
            PmsBaseAttrValue pmsBaseAttrValueDel = new PmsBaseAttrValue();
            //先删除再添加
            pmsBaseAttrValueDel.setAttrId(id);
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValueDel);
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                if(StringUtils.isBlank(pmsBaseAttrValue.getAttrId())){
                    pmsBaseAttrValue.setAttrId(id);
                }
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }

        }

        return "success";
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
        return pmsBaseAttrValues;
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByIds(Set<String> set) {
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = new ArrayList<>();
        if(set!=null && set.size()>0){
            String idsStr = StringUtils.join(set, ",");
            pmsBaseAttrInfos = pmsBaseAttrInfoMapper.getAttrValueListByIds(idsStr);
        }
        return pmsBaseAttrInfos;
    }
}
