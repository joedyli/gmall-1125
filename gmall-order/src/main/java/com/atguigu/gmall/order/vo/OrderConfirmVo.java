package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {
    // 收货地址列表
    private List<UserAddressEntity> addresses;
    // 商品列表
    private List<OrderItemVo> orderItems;
    // 用户积分
    private Integer bounds;
    // 防止订单重复提交（确保下单的幂等性）
    private String orderToken;
}
