package com.atguigu.gmall.oms.service.impl;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.exception.OrderException;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private OrderItemMapper itemMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo) {
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("该订单没有选中的商品信息！");
        }
        // 新增order表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setUserId(submitVo.getUserId());
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(submitVo.getUserId());
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null){
            orderEntity.setUsername(userEntity.getUsername());
        }
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayAmount(submitVo.getTotalPrice().subtract(new BigDecimal(submitVo.getBounds() / 100)));
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        // TODO: 赠送积分，遍历商品清单根据skuId查询商品赠送积分信息

        UserAddressEntity address = submitVo.getAddress();
        if (address != null){
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverAddress(address.getAddress());
            orderEntity.setReceiverRegion(address.getRegion());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverCity(address.getCity());
        }
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());
        orderEntity.setIntegrationAmount(new BigDecimal(submitVo.getBounds() / 100));
        this.save(orderEntity);

        // 新增order_item
        items.forEach(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrderId(orderEntity.getId());
            itemEntity.setOrderSn(submitVo.getOrderToken());
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                itemEntity.setSkuId(item.getSkuId());
                itemEntity.setSkuQuantity(item.getCount().intValue());
                itemEntity.setSkuPrice(skuEntity.getPrice());
                itemEntity.setSkuPic(skuEntity.getDefaultImage());
                itemEntity.setSkuName(skuEntity.getName());
                itemEntity.setCategoryId(skuEntity.getCatagoryId());

                ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
                SpuEntity spuEntity = spuEntityResponseVo.getData();
                itemEntity.setSpuId(skuEntity.getSpuId());
                // TODO：查询品牌名称
                itemEntity.setSpuBrand(skuEntity.getBrandId().toString());
                itemEntity.setSpuName(spuEntity.getName());

                ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
                SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
                itemEntity.setSpuPic(spuDescEntity.getDecript());

                // TODO: 查询sku的销售属性信息
            }
            // TODO：根据skuid查询sku赠送的积分信息

            this.itemMapper.insert(itemEntity);
        });
//        int i = 1/0;

        // 在订单创建完成之后，返回之前发送消息定时关单
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", submitVo.getOrderToken());

        return orderEntity;
    }

}
