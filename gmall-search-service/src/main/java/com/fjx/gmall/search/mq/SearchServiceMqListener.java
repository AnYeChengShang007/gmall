package com.fjx.gmall.search.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.PaymentInfo;
import com.fjx.gmall.bean.PmsSearchSkuInfo;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.PaymentService;
import com.fjx.gmall.service.PmsSkuInfoService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import io.searchbox.indices.DeleteIndex;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 冯金星
 * @date 2020/9/1/0001 上午 10:44
 */

@Component
public class SearchServiceMqListener {

    @Reference
    PmsSkuInfoService skuService;

    @Autowired
    JestClient jestClient;

    @JmsListener(destination = "SKU_ADD_QUEUQE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException, IOException {

        jestClient.execute(new DeleteIndex.Builder("gmall").build());
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

            jestClient.execute(index);
        }
    }
}
