package com.fjx.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.fjx.gmall.annotatioins.LoginRequired;
import com.fjx.gmall.bean.OmsOrder;
import com.fjx.gmall.bean.PaymentInfo;
import com.fjx.gmall.payment.config.AlipayConfig;
import com.fjx.gmall.service.OrderService;
import com.fjx.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    PaymentService paymentService;

    @Reference
    OrderService orderService;

    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap map) {
        String form = null;
        //pagepay请求参数
        Map<String, Object> data = new HashMap();
        data.put("out_trade_no", outTradeNo);
        data.put("product_code", "FAST_INSTANT_TRADE_PAY");
        data.put("total_amount", totalAmount);
        data.put("subject", "金星商城商品");
        String param = JSON.toJSONString(data);
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        //同步回调地址
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setBizContent(param);
        try {
            //获得一个支付宝请求的客户端，并不是一个链接，而是一个封装好的http表单请求
            form = alipayClient.pageExecute(alipayRequest).getBody();
            //生成并且保存用户的支付信息
            OmsOrder order = orderService.getOrderByOutTradeNo(outTradeNo);
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setCreateTime(new Date());
            paymentInfo.setOrderId(order.getId());
            paymentInfo.setOrderSn(outTradeNo);
            paymentInfo.setPaymentStatus("未支付");
            paymentInfo.setSubject("金星商城商品");
            paymentInfo.setConfirmTime(new Date());
            paymentInfo.setTotalAmount(totalAmount);
            paymentService.savePaymentInfo(paymentInfo);
            // 向消息中间件发送一个检查支付状态(支付服务消费)的延迟消息队列
            paymentService.sendDelayPaymentResultCheckQueue(outTradeNo, 5);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return form;
    }

    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String callback(HttpServletRequest request, HttpServletResponse response) {

        // 回调请求中获取支付宝参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String merchant_order_no = request.getParameter("merchant_order_no");
        String call_back_content = request.getQueryString();
        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if (StringUtils.isNotBlank(sign)) {
            // 验签成功
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);// 支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            // 更新用户的支付状态
            paymentService.updatePayment(paymentInfo);
        }
        return "redirect:http://localhost:8086/list";
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap map) {
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
        map.put("memberId", memberId);
        map.put("nickName", nickName);
        map.put("orderId", outTradeNo);
        map.put("totalAmount", totalAmount);

        return "index";
    }

}
