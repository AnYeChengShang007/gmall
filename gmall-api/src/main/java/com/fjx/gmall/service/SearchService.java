package com.fjx.gmall.service;

import com.fjx.gmall.bean.GmallSearchResult;
import com.fjx.gmall.bean.PmsSearchParam;


public interface SearchService {
    GmallSearchResult list(PmsSearchParam pmsSearchParam,boolean pagable);
}
