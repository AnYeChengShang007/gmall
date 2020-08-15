package com.fjx.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.PmsSearchSkuInfo;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.PmsSkuInfoService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * skuInfo数据导入elasticsearch
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallSearchServiceApplicationTests {


    @Reference
    PmsSkuInfoService skuService;

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
        //查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList = skuService.getAllPmsSkuInfos();
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();

        //转化为es数据结构
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);
            pmsSearchSkuInfoList.add(pmsSearchSkuInfo);
        }

        //导入es
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            Index index = new Index.Builder(pmsSearchSkuInfo).index("gmall").type("_doc").id(pmsSearchSkuInfo.getId()).build();
            //put /gmall/_doc/{index}
            //{
            //    .....
            // }
            jestClient.execute(index);
        }
    }

}
