package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.vo.PaymentInfoEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentService {

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    public OrderEntity queryOrder(String orderToken) {

        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrderByOrderToken(orderToken);
        return orderEntityResponseVo.getData();
    }

    public Long savePayment(OrderEntity orderEntity){

        PaymentInfoEntity entity = this.paymentInfoMapper.selectOne(new QueryWrapper<PaymentInfoEntity>().eq("out_trade_no", orderEntity.getOrderSn()));
        if (entity != null){
            return entity.getId();
        }

        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());
        paymentInfoEntity.setCreateTime(new Date());
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setSubject("谷粒商城支付平台");
        paymentInfoEntity.setTotalAmount(new BigDecimal(0.01));

        this.paymentInfoMapper.insert(paymentInfoEntity);

        return paymentInfoEntity.getId();
    }

    public PaymentInfoEntity queryPaymentByPayId(Long payId){
        return this.paymentInfoMapper.selectById(payId);
    }

    public void updatePayStatus(PaymentInfoEntity paymentInfoEntity){
        this.paymentInfoMapper.updateById(paymentInfoEntity);
    }
}
