package com.fjx.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.fjx.gmall.UserConstant;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.bean.OmsCartItem;
import com.fjx.gmall.bean.PmsSkuInfo;
import com.fjx.gmall.service.CartService;
import com.fjx.gmall.service.PmsSkuInfoService;
import com.fjx.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Controller
public class CartController {

    @Reference
    PmsSkuInfoService skuService;

    @Reference
    CartService cartService;


    @RequestMapping("deleteCartItemById")
    @LoginRequired(loginSuccess = false)
    @ResponseBody
    public String deleteCartItemById(String skuId, HttpServletRequest request, HttpServletResponse response) {
        Object mId = request.getAttribute(UserConstant.MEMBERID);
        Object name = request.getAttribute(UserConstant.NICKNAME);
        String memberId = null;
        if (mId != null) {
            memberId = (String) mId;
        }
        if (StringUtils.isNotBlank(memberId)) {
            cartService.delCart(skuId);
            cartService.flushCache(memberId);
            return "success";
        } else {
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                List<OmsCartItem> cartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                for (int i = 0; i < cartItems.size(); i++) {
                    if (cartItems.get(i).getProductSkuId().equals(skuId)) {
                        cartItems.remove(i);
                        break;
                    }
                }
                CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(cartItems), 60 * 60 * 24 * 3, true);
                return "success";
            }

        }
        return "fail";
    }


    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked, String skuId, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        Object mId = request.getAttribute("memberId");
        Object name = request.getAttribute("nickName");
        String memberId = null;
        if (mId != null) {
            memberId = (String) mId;
        }
        if (StringUtils.isNotBlank(memberId)) {
            // 调用服务，修改状态
            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(memberId);
            omsCartItem.setProductSkuId(skuId);
            omsCartItem.setIsChecked(isChecked);
            cartService.checkCart(omsCartItem);

            // 将最新的数据从缓存中查出，渲染给内嵌页
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            BigDecimal allPrice = cartService.getTotalPrice(omsCartItems);
            modelMap.put("cartList", omsCartItems);
            modelMap.put("allPrice", allPrice);
        } else {
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            List<OmsCartItem> cartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            for (OmsCartItem cartItem : cartItems) {
                if (cartItem.getProductSkuId().equals(skuId)) {
                    cartItem.setIsChecked(isChecked);
                }
            }
            BigDecimal allPrice = cartService.getTotalPrice(cartItems);
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(cartItems), 60 * 60 * 24 * 3, true);
            modelMap.put("allPrice", allPrice);
        }
        return "cartListInner";
    }

    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response) {

        List<OmsCartItem> omsCartItems = new ArrayList<>();
        String memberId = (String) request.getAttribute("memberId");
        if (StringUtils.isNotBlank(memberId)) {
            //已经登录查询db
            omsCartItems = cartService.cartList(memberId);
        } else {
            //还未登录查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                //cookie中有值
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            } else {
                //cookie中无值
            }
        }
        BigDecimal allPrice = new BigDecimal(0);
        if (!CollectionUtils.isEmpty(omsCartItems)) {
            allPrice = cartService.getTotalPrice(omsCartItems);
        }
        modelMap.put("cartList", omsCartItems);
        modelMap.put("allPrice", allPrice);
        modelMap.put("userId", memberId);
        return "cartList";
    }


    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(ModelMap modelMap, String skuId, Integer quantity, HttpServletRequest request, HttpServletResponse response) {

        List<OmsCartItem> cartItems = new ArrayList<>();

        //调用商品服务查询信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);
        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setMemberNickname((String) request.getAttribute("nickName"));
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode(UUID.randomUUID().toString());
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(quantity);
        omsCartItem.setIsChecked("1");
        omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(new BigDecimal(omsCartItem.getQuantity())));


        //判断用户是否登录
        Object mId = request.getAttribute("memberId");
        Object name = request.getAttribute("nickName");
        String memberId = null;
        if (mId != null) {
            memberId = (String) mId;
        }
        //根据用户登录决定揍cookie的分支还是db
        if (StringUtils.isNotBlank(memberId)) {
            //用户已经登录
            //从db中查询需要的数据
            OmsCartItem insertCartItem = cartService.findCarItemByUser(memberId, skuId);
            if (insertCartItem == null) {
                //db为空,说明购物车无此商品，进行添加操作
                omsCartItem.setMemberId(memberId);
                cartService.addCart(omsCartItem);
            } else {
                //db不为空,说明购物车里已经添加过次商品，进行修改操作
                insertCartItem.setQuantity(omsCartItem.getQuantity());
                cartService.updateCart(insertCartItem);
            }
            //同步缓存
            cartService.flushCache(memberId);
        } else {
            //用户未登录
            //cookie的跨域问题，set/getDomain()
            //cookie中原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isBlank(cartListCookie)) {
                //cookie为空
                cartItems.add(omsCartItem);
            } else {
                //cookie不为空
                cartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                boolean exist = if_cart_exist(cartItems, omsCartItem);
                if (exist) {
                    //如果之前添加过就更新数量。。
                    for (OmsCartItem cartItem : cartItems) {
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())) {
                            cartItem.setQuantity(omsCartItem.getQuantity());
                        }
                    }
                } else {
                    //之前没有添加，追加到当前购物车
                    cartItems.add(omsCartItem);
                }
            }
            //更新cookie
            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(cartItems), 60 * 60 * 24 * 3, true);
        }
        modelMap.put("skuInfo", skuInfo);
        modelMap.put("quantity", quantity);
        return "success";
    }

    //判断添加的购物车物品在cookie中是否存在
    private boolean if_cart_exist(List<OmsCartItem> cartItems, OmsCartItem omsCartItem) {
        boolean exist = false;
        for (OmsCartItem cartItem : cartItems) {
            if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())) {
                exist = true;
                return exist;
            }
        }
        return exist;
    }

}
