package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "ORDER_DEAD_QUEUE")
    public void closeOrder(String orderToken, Channel channel, Message message) throws IOException {
        try {
            if (this.orderMapper.closeOrder(orderToken) == 1) {
                // 解锁库存
                this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()){
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ORDER_PAY_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.pay"}
    ))
    public void paySuccess(String orderToken, Channel channel, Message message) throws IOException {
        // 更新订单状态
        if (this.orderMapper.successOrder(orderToken) == 1) {
            // 发送消息给wms，完成减库存的操作
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.minus", orderToken);

            // 发送消息给oms，给用户加积分 TODO: 给ums添加监听器更新用户表
            OrderEntity orderEntity = this.orderMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderToken));
            Map<String, Object> map = new HashMap<>();
            map.put("userId", orderEntity.getUserId());
            map.put("growth", orderEntity.getGrowth());
            map.put("integration", orderEntity.getIntegration());
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "bounds.add", map);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
