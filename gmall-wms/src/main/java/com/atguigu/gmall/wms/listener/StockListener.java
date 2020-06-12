package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "wms:stock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_STOCK_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlock(String orderToken, Channel channel, Message message) throws IOException {
        String lockSkuString = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
        if (StringUtils.isNotBlank(lockSkuString)) {
            List<SkuLockVo> skuLockVos = JSON.parseArray(lockSkuString, SkuLockVo.class);

            skuLockVos.forEach(lockVo -> {
                this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
            });

            // 防止重复解锁库存，解锁完成之后要删除锁定库存信息
            this.redisTemplate.delete(KEY_PREFIX + orderToken);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
