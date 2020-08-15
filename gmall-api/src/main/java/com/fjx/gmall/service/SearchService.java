package com.fjx.gmall.service;

import com.fjx.gmall.bean.PmsSearchParam;
import com.fjx.gmall.bean.PmsSearchSkuInfo;

import java.util.List;

public interface SearchService {
    List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam);
}
