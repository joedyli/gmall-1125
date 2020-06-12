package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {
    private Long userId; // 用户id
    private String orderToken; // 防重
    private UserAddressEntity address; // 用户选中的地址信息
    private Integer payType; // 支付方式
    private String deliveryCompany; // 配送方式/快递公司
    private List<OrderItemVo> items; // 订单中的商品列表

    private Integer bounds; // 积分信息
    private BigDecimal totalPrice; // 总价，验价
}
