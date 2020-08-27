package com.fjx.gmall.payment.service.impl;

import com.fjx.gmall.bean.PaymentInfo;
import com.fjx.gmall.payment.mapper.PaymentInfoMaper;
import com.fjx.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

@Service
public class PaymentServiceImpl implements PaymentService {


    @Autowired
    PaymentInfoMaper paymentInfoMaper;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMaper.insert(paymentInfo);
    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        String orderSn = paymentInfo.getOrderSn();

        Example e = new Example(PaymentInfo.class);
        e.createCriteria().andEqualTo("orderSn", orderSn);
        paymentInfoMaper.updateByExampleSelective(paymentInfo, e);

    }

}
