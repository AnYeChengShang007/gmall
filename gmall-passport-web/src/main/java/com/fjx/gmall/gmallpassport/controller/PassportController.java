package com.fjx.gmall.gmallpassport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.bean.OmsCartItem;
import com.fjx.gmall.bean.UmsMember;
import com.fjx.gmall.service.CartService;
import com.fjx.gmall.service.UserService;
import com.fjx.gmall.util.CookieUtil;
import com.fjx.gmall.util.IPUtil;
import com.fjx.gmall.util.JwtUtil;
import com.fjx.gmall.utils.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
class PassportController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @RequestMapping("/")
    public String index(String returnUrl, ModelMap modelMap) {
        modelMap.put("returnUrl", returnUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request,HttpServletResponse response) {
        String token = "";
        //调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin != null) {
            //登陆成功
            String ip = IPUtil.getIp(request);
            //jwt制作token
            token = getToken("gmall", umsMemberLogin, ip);
            //token存入redis一份
            userService.addUserToken(token, umsMemberLogin.getId());

            //登录成功，进行购物车的同步
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                List<OmsCartItem> cartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                for (OmsCartItem cartItem : cartItems) {
                    OmsCartItem cartItemByUser = cartService.findCarItemByUser(umsMemberLogin.getId(), cartItem.getProductSkuId());
                    if (cartItemByUser != null) {
                        cartItemByUser.setQuantity(cartItemByUser.getQuantity() + cartItem.getQuantity());
                        cartService.updateCart(cartItemByUser);
                    } else {
                        cartItem.setMemberId(umsMemberLogin.getId());
                        cartService.addCart(cartItem);
                    }
                }
                cartService.flushCache(umsMemberLogin.getId());
                CookieUtil.deleteCookie(request,response,"cartListCookie");
            }


            return token;
        }
        //登录失败
        token = "fail";
        return token;
    }

    @RequestMapping("logout")
    @ResponseBody
    @CrossOrigin
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String token = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(token)) {
            CookieUtil.setCookie(request, response, "oldToken", token, 0, true);
        }
        return "success";
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

    /**
     * 社交登录
     *
     * @param code
     * @param request
     * @return
     */
    @RequestMapping("vlogin")
    public String vlogin(String code, HttpServletRequest request) {
        //授权码获取access_token;
        Map<String, String> map = new HashMap<String, String>();
        map.put("client_id", "639265612");
        map.put("client_secret", "51d0faaeb509cc5cbc71f9489a066a68");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://127.0.0.1:8085/vlogin");
        map.put("code", code);
        String res = HttpClientUtil.doPost("https://api.weibo.com/oauth2/access_token", map);
        Map<String, String> m = JSON.parseObject(res, Map.class);
        String access_token = m.get("access_token");
        Long uid = new Long(m.get("uid"));

        //access_token获取用户信息
        String userMsgStr = HttpClientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token=" + access_token + "&uid=" + uid);
        Map<String, String> userMsg = JSON.parseObject(userMsgStr, Map.class);
        //将用户信息存入数据库,用户设置为微博用户
        UmsMember user = new UmsMember();
        user.setSourceUid(Long.valueOf((userMsg.get("idstr"))));
        UmsMember umsMemberCheck = userService.checkAuthUser(user);//检查次用户是否登陆过
        user.setSourceType(2);
        user.setAccessCode(access_token);
        user.setAccessToken(access_token);
        user.setCity(userMsg.get("location"));
        user.setNickname(userMsg.get("name"));
        String sex = userMsg.get("gender");
        int gender = 0;
        if (sex.equals("m")) {
            gender = 1;
        } else if (sex.equals("f")) {
            gender = 2;
        }
        user.setGender(gender);
        if (umsMemberCheck == null) {
            //如果不存在此用户则保存
            //mybatis主键返回策略无法通过RPC，这里返回user对象
            user = userService.addOAuthUser(user);
        } else {
            user = umsMemberCheck;
        }


        //生成jwt的token，并且重定向到首页，携带token
        String ip = IPUtil.getIp(request);
        String token = getToken("gmall", user, ip);
        return "redirect:http://localhost:8083/index?token=" + token;
    }


    public String getToken(String key, UmsMember umsMember, String ip) {
        String memberId = umsMember.getId();//主键返回策略不能跨越RPC
        String nickname = umsMember.getNickname();
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("memberId", memberId);
        userMap.put("nickName", nickname);
        //生产环境中需要对参数进行加密，生成token
        String token = JwtUtil.encode(JwtUtil.KEY, userMap, ip);
        return token;
    }


}