package com.fjx.gmall.util;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class IPUtil {

    public static String getIp(HttpServletRequest request){
        //如果是nginx转发，可获得客户端源ip
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip)) {
            //不是nginx代理，从request获取ip
            ip = request.getRemoteAddr();
        }
        if (StringUtils.isBlank(ip)) {
            //非法请求
            throw new RuntimeException("非法请求，获取不到ip");
        }
        //return ip;
        return "localhost";
    }
}
