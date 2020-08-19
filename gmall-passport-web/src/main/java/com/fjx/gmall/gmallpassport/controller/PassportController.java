package com.fjx.gmall.gmallpassport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.UmsMember;
import com.fjx.gmall.service.UserService;
import com.fjx.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("/")
    public String index(String returnUrl, ModelMap modelMap) {
        modelMap.put("returnUrl", returnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request) {
        String token = "";

        //调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin != null) {
            //登陆成功

            //jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("mwmberId",memberId);
            userMap.put("nickname",nickname);
            //如果是nginx转发，可获得客户端源ip
            String ip = request.getHeader("X-Forwarded-For");
            if(StringUtils.isBlank(ip)){
                //不是nginx代理，从request获取ip
                ip = request.getRemoteAddr();
            }
            if(StringUtils.isBlank(ip)){
                //非法请求
                throw new RuntimeException("非法请求，获取不到ip");
            }

            token = JwtUtil.encode("gmall", userMap, ip);

            //token存入redis一份
            userService.addUserToken(token,memberId);
            return token;
        }
        //登录失败
        token = "fail";
        return token;
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,String ip) {
        //通过jwt验证token真假
        Map<String,String> map = new HashMap<>();
        Map<String, Object> decode = JwtUtil.decode(token, "gmall", ip);
        if(decode!=null){
            map.put("status","success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickName", (String) decode.get("nickName"));
        }else{
            map.put("status","fail");
        }
        return JSON.toJSONString(map);
    }

}