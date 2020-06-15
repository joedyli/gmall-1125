package com.atguigu.gmall.payment.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.exception.OrderException;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import com.atguigu.gmall.payment.vo.PaymentInfoEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Date;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("pay.html")
    public String paySelect(@RequestParam("orderToken")String orderToken, Model model){

        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        model.addAttribute("orderEntity", orderEntity);
        return "pay";
    }

    @GetMapping("alipay.html")
    @ResponseBody
    public String alipay(@RequestParam("orderToken")String orderToken){
        String form = null;

        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity.getStatus() != 0){
            throw new OrderException("该订单无法支付，请查询订单状态");
        }
        try {
            // 调用阿里的接口，跳转到阿里支付页面
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderToken);
            // 千万不要填商品的真是金额，请写0.01
//            payVo.setTotal_amount(orderEntity.getPayAmount().toString());
            payVo.setTotal_amount("0.01");
            payVo.setSubject("谷粒商城支付平台");
            // 对支付进行记录，方便跟支付宝对账，也方便将来校验业务参数
            Long payId = this.paymentService.savePayment(orderEntity);
            payVo.setPassback_params(payId.toString());
            form = this.alipayTemplate.pay(payVo);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return form;
    }

    @GetMapping("pay/return")
    public String payReturn(PayAsyncVo payAsyncVo, Model model){
        model.addAttribute("total_amount", payAsyncVo.getTotal_amount());
        return "paysuccess";
    }

    @PostMapping("pay/success")
    @ResponseBody
    public String paySuccess(PayAsyncVo payAsyncVo){
        // 1.验签
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag){
            return "failure";
        }

        // 2.校验业务数据:app_id、out_trade_no、total_amount。根据
        String payId = payAsyncVo.getPassback_params();
        if (StringUtils.isBlank(payId)){
            return "failure";
        }
        PaymentInfoEntity paymentInfoEntity = this.paymentService.queryPaymentByPayId(Long.valueOf(payId));
        if (paymentInfoEntity == null
                || !StringUtils.equals(payAsyncVo.getOut_trade_no(), paymentInfoEntity.getOutTradeNo())
                || paymentInfoEntity.getTotalAmount().compareTo(new BigDecimal(payAsyncVo.getBuyer_pay_amount())) != 0){
            return "failure";
        }

        // 3.校验交易状态码：TRADE_SUCCESS
        if (!StringUtils.equals("TRADE_SUCCESS", payAsyncVo.getTrade_status())){
            return "failure";
        }

        // 4.记录支付状态
        paymentInfoEntity.setPaymentStatus(1); // 把对账表的支付状态记录为支付成功状态（0 ---》 1）
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        this.paymentService.updatePayStatus(paymentInfoEntity);

        // 5.更新订单状态 减库存.TODO
        // 方式一：oms提供同步接口更新状态
        // 方式二：MQ异步方式更新订单状态
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.pay", payAsyncVo.getOut_trade_no());

        // 6.响应支付宝需要的状态给支付宝
        return "success";
    }

}
