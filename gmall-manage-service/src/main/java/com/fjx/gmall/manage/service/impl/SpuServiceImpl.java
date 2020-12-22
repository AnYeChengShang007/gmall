package com.fjx.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.fjx.gmall.bean.PmsProductImage;
import com.fjx.gmall.bean.PmsProductInfo;
import com.fjx.gmall.bean.PmsProductSaleAttr;
import com.fjx.gmall.bean.PmsProductSaleAttrValue;
import com.fjx.gmall.manage.mapper.PmsProductImageMapper;
import com.fjx.gmall.manage.mapper.PmsProductInfoMapper;
import com.fjx.gmall.manage.mapper.PmsProductSaleAttrMapper;
import com.fjx.gmall.manage.mapper.PmsProductSaleAttrValueMapper;
import com.fjx.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;

    @Autowired
    PmsProductSaleAttrMapper pmsProductSaleAttrMapper;

    @Autowired
    PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;

    @Autowired
    PmsProductImageMapper pmsProductImageMapper;


    //查询所有商品信息
    @Override
    public List<PmsProductInfo> spuList(String catalog3Id) {
        PmsProductInfo pmsProductInfo = new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        List<PmsProductInfo> pmsProductInfos =
                pmsProductInfoMapper.select(pmsProductInfo);
        return pmsProductInfos;
    }

    //保存商品信息
    @Override
    public void saveSpuInfo(PmsProductInfo pmsProductInfo) {

        //保存商品信息
        pmsProductInfoMapper.insert(pmsProductInfo);

//        商品id
        String id = pmsProductInfo.getId();

//        保存商品销售属性
        List<PmsProductSaleAttr> productSaleAttrList =
                pmsProductInfo.getPmsProductSaleAttrList();
        for (PmsProductSaleAttr productSaleAttr : productSaleAttrList) {
            productSaleAttr.setProductId(id);
            pmsProductSaleAttrMapper.insert(productSaleAttr);
//            保存商品销售属性值
            List<PmsProductSaleAttrValue> productSaleAttrValueList =
                    productSaleAttr.getPmsProductSaleAttrValueList();
            for (PmsProductSaleAttrValue pmsProductSaleAttrValue : productSaleAttrValueList) {
                pmsProductSaleAttrValue.setProductId(id);
                pmsProductSaleAttrValueMapper.insert(pmsProductSaleAttrValue);
            }
        }


//        保存商品图片
        List<PmsProductImage> productImageList = pmsProductInfo.getPmsProductImageList();
        for (PmsProductImage productImage : productImageList) {
            productImage.setProductId(id);
            pmsProductImageMapper.insert(productImage);
        }

    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);
        //获得商品销售属性列表
        List<PmsProductSaleAttr> pmsProductSaleAttrList =
                pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
        for (PmsProductSaleAttr productSaleAttr : pmsProductSaleAttrList) {
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setProductId(spuId);
            pmsProductSaleAttrValue.setSaleAttrId(productSaleAttr.getSaleAttrId());
            //获得商品销售属性值列表
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValueList =
                    pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
            productSaleAttr.setPmsProductSaleAttrValueList(pmsProductSaleAttrValueList);
        }
        return pmsProductSaleAttrList;
    }

    @Override
    public List<PmsProductImage> spuImageList(String spuId) {
        PmsProductImage pmsProductImage = new PmsProductImage();
        pmsProductImage.setProductId(spuId);
        List<PmsProductImage> pmsProductImageList = pmsProductImageMapper.select(pmsProductImage);
        return pmsProductImageList;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId, String skuId) {
        List<PmsProductSaleAttr> pmsProductSaleAttrList = pmsProductSaleAttrMapper.spuSaleAttrListCheckBySku(productId, skuId);
        return pmsProductSaleAttrList;
    }

}
