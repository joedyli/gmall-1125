package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallOmsApi {

    @PostMapping("oms/order/save/order")
    public ResponseVo<OrderEntity> saveOrder(@RequestBody OrderSubmitVo submitVo);

    @GetMapping("oms/order/query/{orderToken}")
    public ResponseVo<OrderEntity> queryOrderByOrderToken(@PathVariable("orderToken")String orderToken);
}
