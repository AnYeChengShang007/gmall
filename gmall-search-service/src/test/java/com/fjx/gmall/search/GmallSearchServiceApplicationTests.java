package com.fjx.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.bean.PmsSearchSkuInfo;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.PmsSkuInfoService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
            jestClient.execute(index);
        }
    }

    @Test
    public void contextLoads2() throws IOException {

        Search search = new Search.Builder("{\n" +
                "  \n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "\n" +
                "      \"filter\": [\n" +
                "        {\n" +
                "          \"terms\": {\n" +
                "            \"skuAttrValueList.attrId\": [\n" +
                "              \"43\"\n" +
                "            ]\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"term\": {\n" +
                "            \"skuAttrValueList.attrId\": \"43\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "           \"term\": {\n" +
                "            \"skuAttrValueList.valueId\": \"114\"\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "    \n" +
                "  }\n" +
                "}").addIndex("gmall").addType("_doc").build();
        SearchResult execute = jestClient.execute(search);
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;

        }
    }
    @Test
    public void contextLoads3() throws IOException {

        //jest的DSL工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            //filter
        TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("skuAttrValueList.attrId",43);
        TermQueryBuilder termQueryBuilder2 = new TermQueryBuilder("skuAttrValueList.valueId",114);
        boolQueryBuilder.filter(termQueryBuilder1);
        boolQueryBuilder.filter(termQueryBuilder2);
        //must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","iqoo");
        BoolQueryBuilder must = boolQueryBuilder.must(matchQueryBuilder);
        //query
        searchSourceBuilder.query(boolQueryBuilder);
        //from
        searchSourceBuilder.from(0);
        //size
        searchSourceBuilder.size(10);
        //hightlight
        //searchSourceBuilder.highlight(null);

        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex("gmall").build();
        SearchResult execute = jestClient.execute(search);
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;

        }
    }

}
