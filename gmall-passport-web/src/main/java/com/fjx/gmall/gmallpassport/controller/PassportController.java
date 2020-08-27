package com.fjx.gmall.gmallpassport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.UmsMember;
import com.fjx.gmall.service.UserService;
import com.fjx.gmall.util.IPUtil;
import com.fjx.gmall.util.JwtUtil;
import com.fjx.gmall.utils.HttpclientUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
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
            String ip = IPUtil.getIp(request);
            //jwt制作token
            token = getToken("gmall", umsMemberLogin,ip);
            //token存入redis一份
            userService.addUserToken(token, umsMemberLogin.getId());
            return token;
        }
        //登录失败
        token = "fail";
        return token;
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token, String currentIp) {
        //通过jwt验证token真假
        Map<String, String> map = new HashMap<>();
        Map<String, Object> decode = JwtUtil.decode(token, "gmall", currentIp);
        if (decode != null) {
            map.put("status", "success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickName", (String) decode.get("nickName"));
        } else {
            map.put("status", "fail");
        }
        return JSON.toJSONString(map);
    }

    @RequestMapping("vlogin")
    public String vlogin(String code,HttpServletRequest request) {
        System.out.println(code);
        System.out.println(request.getRequestURL());
        System.out.println(request.getRemoteAddr());
        //授权码获取access_token;
        Map<String,String> map = new HashMap<String, String>();
        map.put("client_id","639265612");
        map.put("client_secret","51d0faaeb509cc5cbc71f9489a066a68");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://127.0.0.1:8085/vlogin");
        map.put("code",code);
        String res = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token", map);
        Map<String,String> m = JSON.parseObject(res, Map.class);
        String access_token = m.get("access_token");
        Long uid = new Long(m.get("uid"));

        //access_token获取用户信息
        String userMsgStr = HttpclientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid);
        Map<String,String> userMsg = JSON.parseObject(userMsgStr, Map.class);
        //将用户信息存入数据库,用户设置为微博用户
        UmsMember user = new UmsMember();
        user.setSourceUid(new Long((userMsg.get("idstr"))));
        UmsMember umsMemberCheck = userService.checkAuthUser(user);//检查次用户是否登陆过
        user.setSourceType(2);
        user.setAccessCode(access_token);
        user.setAccessToken(access_token);
        user.setCity(userMsg.get("location"));
        user.setNickname(userMsg.get("name"));
        String sex = userMsg.get("gender");
        int gender = 0;
        if(sex.equals("m")){
            gender = 1;
        }else if(sex.equals("f")){
            gender = 2;
        }
        user.setGender(gender);
        if(umsMemberCheck==null){//如果不存在此用户则保存
            user = userService.addOAuthUser(user);
        }else{
            user = umsMemberCheck;
        }


        //生成jwt的token，并且重定向到首页，携带token
        String ip = IPUtil.getIp(request);
        String token = getToken("gmall", user, ip);
        return "redirect:http://localhost:8083/index?token="+token;
    }


    public String getToken(String key,UmsMember umsMember,String ip){
        String memberId = umsMember.getId();//主键返回策略不能跨越RPC
        String nickname = umsMember.getNickname();
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("memberId", memberId);
        userMap.put("nickName", nickname);
        String token = JwtUtil.encode(key, userMap, ip);
        return token;
    }


}