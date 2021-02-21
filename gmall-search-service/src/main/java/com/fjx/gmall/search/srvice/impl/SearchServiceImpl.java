package com.fjx.gmall.search.srvice.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.fjx.gmall.bean.*;
import com.fjx.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    private static int SIZE = 20;

    @Autowired
    JestClient jestClient;

    @Override
    public GmallSearchResult list(PmsSearchParam pmsSearchParam, boolean pagable) {

        GmallSearchResult gmallSearchResult = new GmallSearchResult();

        String dslStr = getSearchDsl(pmsSearchParam, pagable);

        // 用api执行复杂查询
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();
        Search search = new Search.Builder(dslStr).addIndex("gmall").build();
        SearchResult execute = null;
        try {
            execute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;
            Map<String, List<String>> highlight = hit.highlight;
            if (highlight != null) {
                String skuName = highlight.get("skuName").get(0);
                source.setSkuName(skuName);
            }
            pmsSearchSkuInfos.add(source);
        }
        gmallSearchResult.setSearchSkuInfos(pmsSearchSkuInfos);
        PageInfo pageInfo = new PageInfo();
        pageInfo.setCurrentPage(pmsSearchParam.getPage() == null ? 0 : pmsSearchParam.getPage());
        pageInfo.setSize(SIZE);
        int total = execute.getJsonObject().getAsJsonObject("hits").getAsJsonObject("total").get("value").getAsInt();
        pageInfo.setTotal(total);
        pageInfo.setLastPage(total / SIZE + ((total % SIZE) == 0 ? -1 : 0));
        pageInfo.setTotalPage(total / SIZE + ((total % SIZE) == 0 ? 0 : 1));
        pageInfo.setPages();
        gmallSearchResult.setPageInfo(pageInfo);
        return gmallSearchResult;
    }

    private String getSearchDsl(PmsSearchParam pmsSearchParam, boolean pagable) {
        List<PmsSkuAttrValue> skuAttrValueList = pmsSearchParam.getSkuAttrValueList();

        // jest的dsl工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        // filter
        if (StringUtils.isNotBlank(pmsSearchParam.getCatalog3Id())) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", pmsSearchParam.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if (skuAttrValueList != null) {
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", pmsSkuAttrValue.getValueId());
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // must
        if (StringUtils.isNotBlank(pmsSearchParam.getKeyword())) {
            MultiMatchQueryBuilder multiMatchQueryBuilder = new MultiMatchQueryBuilder(pmsSearchParam.getKeyword(), "skuName", "skuDesc");
            //MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", pmsSearchParam.getKeyword());
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }
        // query
        searchSourceBuilder.query(boolQueryBuilder);
        if (pagable) {
            // from
            if (pmsSearchParam.getPage() != null) {
                searchSourceBuilder.from(pmsSearchParam.getPage() * SIZE);
            } else {
                searchSourceBuilder.from(0);
            }
            // size
            searchSourceBuilder.size(SIZE);
        }

        // highlight
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        String dslStr = searchSourceBuilder.toString();
        return dslStr;
    }
}
