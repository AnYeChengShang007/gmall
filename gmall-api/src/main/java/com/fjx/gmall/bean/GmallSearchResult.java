package com.fjx.gmall.bean;

import java.io.Serializable;
import java.util.List;

public class GmallSearchResult implements Serializable {

    private List<PmsSearchSkuInfo> searchSkuInfos;

    PageInfo pageInfo;

    public List<PmsSearchSkuInfo> getSearchSkuInfos() {
        return searchSkuInfos;
    }

    public void setSearchSkuInfos(List<PmsSearchSkuInfo> searchSkuInfos) {
        this.searchSkuInfos = searchSkuInfos;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }
}
