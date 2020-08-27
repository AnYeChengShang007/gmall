package com.fjx.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.bean.OmsCartItem;
import com.fjx.gmall.bean.OmsOrder;
import com.fjx.gmall.bean.OmsOrderItem;
import com.fjx.gmall.bean.UmsMemberReceiveAddress;
import com.fjx.gmall.service.CartService;
import com.fjx.gmall.service.OrderService;
import com.fjx.gmall.service.PmsSkuInfoService;
import com.fjx.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;

    @Reference
    UserService userService;

    @Reference
    OrderService orderService;

    @Reference
    PmsSkuInfoService skuService;

    //必须登录才能通过
    @LoginRequired(loginSuccess = true)
    @RequestMapping("submitOrder")
    public ModelAndView submitOrder(String tradeCode, String receieveAddressId,
                                    BigDecimal allPrice, HttpServletRequest request,
                                    HttpServletResponse response, ModelMap modelMap) {
        ModelAndView mv = new ModelAndView();

        Object mId = request.getAttribute("memberId");
        Object name = request.getAttribute("nickName");
        String memberId = null;
        String nickName = null;
        if (mId != null) {
            memberId = (String) mId;
        }
        if (name != null) {
            nickName = (String) name;
        }
        //检查交易码,保证一次订单页面只能提交一次
        boolean success = orderService.checkTradeCode(memberId, tradeCode);
        if (success) {

            //根据用户id获取要购买的商品列表，和总价格
            OmsOrder order = new OmsOrder();

            order.setAutoConfirmDay(7);
            order.setCreateTime(new Date());
            order.setDiscountAmount(null);
            //omsOrder.setFreightAmount(); 运费，支付后，在生成物流信息时
            order.setMemberId(memberId);
            order.setMemberUsername(nickName);
            order.setNote("快点发货");
            String outTradeNo = "gmall";
            outTradeNo = outTradeNo + System.currentTimeMillis();// 将毫秒时间戳拼接到外部订单号
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo = outTradeNo + sdf.format(new Date());// 将时间字符串拼接到外部订单号

            order.setOrderSn(outTradeNo);//外部订单号
            order.setPayAmount(allPrice);
            order.setOrderType(1);
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receieveAddressId);
            order.setReceiverCity(umsMemberReceiveAddress.getCity());
            order.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            order.setReceiverName(umsMemberReceiveAddress.getName());
            order.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            order.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            order.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            order.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            // 当前日期加一天，一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            order.setReceiveTime(time);
            order.setSourceType(0);
            order.setStatus(0);
            order.setOrderType(0);
            order.setTotalAmount(allPrice);

            List<OmsOrderItem> orderItems = new ArrayList<>();
            List<OmsCartItem> cartItems = cartService.cartList(memberId);
            for (OmsCartItem cartItem : cartItems) {
                if (cartItem.getIsChecked().equals("1")) {
                    OmsOrderItem orderItem = new OmsOrderItem();

                    //验价格
                    boolean priceCheckSuccess = skuService.checkPrice(cartItem.getProductSkuId(),cartItem.getPrice());
                    if (!priceCheckSuccess) {
                        mv.setViewName("fail");
                        return mv;
                    }
                    // 验库存------远程调用库存系统
                    orderItem.setProductPic(cartItem.getProductPic());
                    orderItem.setProductName(cartItem.getProductName());
                    orderItem.setOrderSn(outTradeNo);// 外部订单号，用来和其他系统进行交互，防止重复
                    orderItem.setProductCategoryId(cartItem.getProductCategoryId());
                    orderItem.setProductPrice(cartItem.getPrice());
                    orderItem.setRealAmount(cartItem.getTotalPrice());
                    orderItem.setProductQuantity(cartItem.getQuantity());
                    orderItem.setProductSkuCode("111111111111");
                    orderItem.setProductSkuId(cartItem.getProductSkuId());
                    orderItem.setProductId(cartItem.getProductId());
                    orderItem.setProductSn("仓库对应的商品编号");// 在仓库中的skuId
                    orderItems.add(orderItem);
                }
            }
            order.setOrderItems(orderItems);


            //将订单和订单详情写入数据库
            //删除购物车的对应商品
            orderService.saveOrder(order);

            //重定向到支付系统
            mv.setViewName("redirect:http://localhost:8087/index");
            //支付系统通过授权过滤器可以拿到用户信息进而查询数据
            mv.addObject("outTradeNo",outTradeNo);
            mv.addObject("totalAmount",allPrice);
            return mv;

        }
        mv.setViewName("fail");
        return mv;
    }


    //必须登录才能通过
    @LoginRequired(loginSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap) {
        //这边认证拦截器会拦截
        Object mId = request.getAttribute("memberId");
        Object name = request.getAttribute("nickName");
        String memberId = null;
        String nickName = null;
        if (mId != null) {
            memberId = (String) mId;
        }
        if (name != null) {
            nickName = (String) name;
        }
        //获取购物车物品集合
        List<OmsCartItem> cartItems = cartService.cartList(memberId);
        //将集合转化为页面结算清单集合
        List<OmsOrderItem> orderItems = new ArrayList<>();
        BigDecimal allPrice = new BigDecimal("0");
        for (OmsCartItem cartItem : cartItems) {
            if (cartItem.getIsChecked().equals("1")) {
                //每循环一个购物车对象就封装一个购物车商品的OmsCartItem
                OmsOrderItem orderItem = new OmsOrderItem();
                orderItem.setProductName(cartItem.getProductName());
                orderItem.setProductSkuId(cartItem.getProductSkuId());
                orderItem.setProductSkuCode(cartItem.getProductSkuCode());
                orderItem.setProductPic(cartItem.getProductPic());
                orderItem.setProductQuantity(cartItem.getQuantity());
                orderItems.add(orderItem);
                //计算总价格
                Integer quantity = cartItem.getQuantity();
                BigDecimal price = cartItem.getPrice();
                BigDecimal totalPrice = price.multiply(new BigDecimal(quantity));
                cartItem.setTotalPrice(totalPrice);
                allPrice = allPrice.add(totalPrice);
            }

        }

        //获得用户地址列表
        List<UmsMemberReceiveAddress> userAddressList = userService.getReceiveAddressByMemberId(memberId);
        //生成交易码
        String tradeCode = orderService.getTradeCode(memberId);

        modelMap.put("orderDetailList", orderItems);
        modelMap.put("userAddressList", userAddressList);
        modelMap.put("allPrice", allPrice);
        modelMap.put("nickName", request.getAttribute("nickName"));
        modelMap.put("tradeCode", tradeCode);
        return "trade";
    }

}
