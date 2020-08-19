package com.fjx.gmall.interceptor;

import com.alibaba.fastjson.JSON;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.util.CookieUtil;
import com.fjx.gmall.utils.HttpclientUtil;
import com.sun.corba.se.impl.oa.toa.TOA;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断请求的访问的方法的注解（是否需要拦截）
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequired loginRequired = handlerMethod.getMethodAnnotation(LoginRequired.class);
        if (loginRequired == null) {
            //此方法没有loginRequired注解，不进行拦截
            return true;
        } else {
            //进行拦截
            String token = "";
            String oldToken = CookieUtil.getCookieValue(request, "token", true);
            if (StringUtils.isNotBlank(oldToken)) {
                token = oldToken;
            }
            String newToken = request.getParameter("token");
            if (StringUtils.isNotBlank(newToken)) {
                token = newToken;
            }
            Map<String,String> map = new HashMap<>();
            String success = "fail";
            if(StringUtils.isNotBlank(token)){
                //调用验证中心进行验证
                String ip = request.getHeader("X-Forwarded-For");
                if(StringUtils.isBlank(ip)){
                    //不是nginx代理，从request获取ip
                    ip = request.getRemoteAddr();
                }
                if(StringUtils.isBlank(ip)){
                    //非法请求
                    throw new RuntimeException("非法请求，获取不到ip");
                }
                String successJson = HttpclientUtil.doGet("http://localhost:8085/verify?token=" + token+"&currentIp="+ip);
                map = JSON.parseObject(successJson, Map.class);
                success = map.get("status");
            }

            boolean loginSuccess = loginRequired.loginSuccess();//获取该请求是否必须要求登录成功
            if (loginSuccess) {
                //如果必须登录成功才能使用
                if (!success.equals("success")) {
                    //验证失败，覆盖cookie中的token
                    //重定向回passport
                    StringBuffer requestURL = request.getRequestURL();
                    response.sendRedirect("http://localhost:8085?returnUrl="+requestURL);
                    return false;
                }
                request.setAttribute("memberId", map.get("memberId"));
                request.setAttribute("nickName", map.get("nickName"));

            } else {
                //可以不登录，但是必须验证
                if (success.equals("success")) {
                    //验证成功，需要将token携带的信息写入
                    request.setAttribute("memberId", map.get("memberId"));
                    request.setAttribute("nickName", map.get("nickName"));
                }
            }

            //如果token不为空并且token认证为success就将token写入cookie
            if(StringUtils.isNotBlank(token) && success.equals("success"))
                CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);


        }
        return true;

    }
}
