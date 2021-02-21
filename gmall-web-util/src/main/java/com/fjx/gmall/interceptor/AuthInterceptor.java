package com.fjx.gmall.interceptor;

import com.alibaba.fastjson.JSON;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.util.CookieUtil;
import com.fjx.gmall.util.IPUtil;
import com.fjx.gmall.utils.HttpClientUtil;
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

    private static String SUCCESS = "success";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断请求的访问的方法的注解（是否需要拦截）
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequired loginRequired = handlerMethod.getMethodAnnotation(LoginRequired.class);
        if (loginRequired == null) {
            //此方法没有loginRequired注解，不进行拦截
            return true;
        }
        //进行拦截
        String token = "";
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }
        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }
        Map<String, String> map = new HashMap<>();
        String success = "fail";

        //验证token
        if (StringUtils.isNotBlank(token)) {
            //调用验证中心进行验证
            String ip = IPUtil.getIp(request);
            String successJson = HttpClientUtil.doGet("http://localhost:8085/verify?token=" + token + "&currentIp=" + ip);
            map = JSON.parseObject(successJson, Map.class);
            success = map.get("status");
        }
        boolean loginSuccess = loginRequired.loginSuccess();//是否必须登录成功
        if (loginSuccess) {
            //如果必须登录成功才能使用
            if (!success.equals(SUCCESS)) {
                //验证失败，覆盖cookie中的token
                //重定向回passport
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://localhost:8085?returnUrl=" + requestURL);
                return false;
            }
        }
        //可以不登录，但是必须验证
        if (success.equals(SUCCESS)) {
            //验证成功，需要将token携带的信息写入
            request.setAttribute("memberId", map.get("memberId"));
            request.setAttribute("nickName", map.get("nickName"));
        }

        //如果token认证为success就将token写入cookie
        if (success.equals(SUCCESS))
            CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
        return true;

    }
}
