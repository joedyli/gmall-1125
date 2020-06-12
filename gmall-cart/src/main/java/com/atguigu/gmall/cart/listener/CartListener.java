package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.KeyPairGenerator;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PRICE_PREFIX = "cart:price:";

    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private CartAsyncService cartAsyncService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_ITEM_QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL_ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        ResponseVo<List<SkuEntity>> listResponseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = listResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuEntities)){
            skuEntities.forEach(skuEntity -> {
                String skuPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + skuEntity.getId());
                if (StringUtils.isNotBlank(skuPrice)){
                    this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
                }
            });
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_CART_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key ={"cart.delete"}
    ))
    public void deleteCart(Map<String, Object> map, Channel channel, Message message) throws IOException {
        String userId = map.get("userId").toString();
        String skuIdString = map.get("skuIds").toString();
        List<Long> skuIds = JSON.parseArray(skuIdString, Long.class);

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        hashOps.delete(skuIds.stream().map(skuId -> skuId.toString()).collect(Collectors.toList()).toArray());

        this.cartAsyncService.deleteCartByUserIdAndSkuIds(userId, skuIds);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
