package com.fjx.gmall.manage.mapper;

import com.fjx.gmall.bean.PmsBaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PmsBaseAttrInfoMapper extends Mapper<PmsBaseAttrInfo> {
    List<PmsBaseAttrInfo> getAttrValueListByIds(@Param("idsStr") String idsStr);
}
