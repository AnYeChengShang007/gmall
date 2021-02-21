package com.fjx.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.bean.OmsCartItem;
import com.fjx.gmall.bean.OmsOrder;
import com.fjx.gmall.bean.OmsOrderItem;
import com.fjx.gmall.bean.TotalPriceAndOrderItems;
import com.fjx.gmall.bean.UmsMemberReceiveAddress;
import com.fjx.gmall.service.CartService;
import com.fjx.gmall.service.OrderService;
import com.fjx.gmall.service.PmsSkuInfoService;
import com.fjx.gmall.service.UserService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
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
                                    HttpServletRequest request, String note,
                                    ModelAndView mv) {
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
            order.setAutoConfirmDay(7);//自动确认收货时间为 7 天
            order.setCreateTime(new Date());
            order.setDiscountAmount(null);
            order.setMemberUsername(nickName);
            //omsOrder.setFreightAmount(); 运费，支付后生成物流信息产生
            order.setMemberId(memberId);
            order.setNote(note);
            String outTradeNo = "gmall";
            outTradeNo = outTradeNo + System.currentTimeMillis();// 将毫秒时间戳拼接到外部订单号
            outTradeNo = outTradeNo +
                    DateFormatUtils.format(new Date(), "YYYYMMDDHHmmss");// 将时间字符串拼接到外部订单号
            order.setOrderSn(outTradeNo);//外部订单号
            order.setOrderType(0);
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receieveAddressId);
            order.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            order.setReceiverCity(umsMemberReceiveAddress.getCity());
            order.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            order.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            order.setReceiverName(umsMemberReceiveAddress.getName());
            order.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            order.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
           /* Calendar calendar = Calendar.getInstance();
              calendar.add(Calendar.DATE, 1);
              Date time = calendar.getTime();
              order.setDeliveryTime(time);//当前日期加一天，一天后配送
            */
            order.setDeleteStatus(0);//订单删除状态
            order.setConfirmStatus(0);//收获确认状态
            order.setSourceType(0);//订单来源：0->PC订单；1->app订单
            order.setStatus(0);
            order.setOrderType(0);
            List<OmsOrderItem> orderItems = new ArrayList<>();
            List<OmsCartItem> cartItems = cartService.cartList(memberId);
            BigDecimal allPrice = cartService.getTotalPrice(cartItems);
            order.setPayAmount(allPrice);
            order.setTotalAmount(allPrice);
            for (OmsCartItem cartItem : cartItems) {
                if (cartItem.getIsChecked().equals("1")) {
                    OmsOrderItem orderItem = new OmsOrderItem();

                    //验价格
                    boolean priceCheckSuccess = skuService.checkPrice(cartItem.getProductSkuId(), cartItem.getPrice());
                    if (!priceCheckSuccess) {
                        mv.setViewName("tradeFail");
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
                    orderItem.setProductSkuCode(cartItem.getProductSkuId());
                    orderItem.setProductSkuId(cartItem.getProductSkuId());
                    orderItem.setProductId(cartItem.getProductId());
                    orderItem.setProductSn("uuid");// 在仓库中的skuId
                    orderItem.setSp1(cartItem.getSp1());
                    orderItem.setSp2(cartItem.getSp2());
                    orderItem.setSp3(cartItem.getSp3());
                    orderItems.add(orderItem);
                }
            }
            order.setOrderItems(orderItems);
            //将订单和订单详情写入数据库并删除购物车的对应商品
            orderService.saveOrder(order);
            //重定向到支付系统
            mv.setViewName("redirect:http://localhost:8087/index");
            //支付系统通过授权过滤器可以拿到用户信息进而查询数据
            mv.addObject("outTradeNo", outTradeNo);
            mv.addObject("totalAmount", allPrice);
            return mv;

        }
        mv.setViewName("tradeFail");
        return mv;
    }

    @RequestMapping("tradeFail")
    String tradeFail() {
        return "tradeFail";
    }

    @RequestMapping("list")
    @LoginRequired(loginSuccess = true)
    public ModelAndView orderList(HttpServletRequest request, ModelAndView mv) {
        Object mId = request.getAttribute("memberId");
        String memberId = null;
        if (mId != null) {
            memberId = (String) mId;
        }
        List<OmsOrder> orderList = orderService.getOrderListByUserId(memberId);
        mv.addObject("orderList", orderList);
        mv.setViewName("list");
        return mv;
    }


    //必须登录才能通过
    @LoginRequired(loginSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap modelMap) {
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
        TotalPriceAndOrderItems totalPriceAndOrderItems = null;
        if (!CollectionUtils.isEmpty(cartItems)) {
            totalPriceAndOrderItems = orderService.getTotalPrice(cartItems);
        } else {
            totalPriceAndOrderItems = new TotalPriceAndOrderItems();
        }
        //获得用户地址列表
        List<UmsMemberReceiveAddress> userAddressList = userService.getReceiveAddressByMemberId(memberId);
        //生成交易码
        String tradeCode = orderService.getTradeCode(memberId);

        modelMap.put("orderDetailList", totalPriceAndOrderItems.getOrderItems());
        modelMap.put("userAddressList", userAddressList);
        modelMap.put("allPrice", totalPriceAndOrderItems.getAllPrice());
        modelMap.put("nickName", request.getAttribute("nickName"));
        modelMap.put("tradeCode", tradeCode);
        return "trade";
    }

}
