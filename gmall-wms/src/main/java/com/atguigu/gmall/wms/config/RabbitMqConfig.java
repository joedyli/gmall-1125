package com.atguigu.gmall.wms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class RabbitMqConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        this.rabbitTemplate.setConfirmCallback(this);
        this.rabbitTemplate.setReturnCallback(this);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack){
            log.info("消息到达了交换机");
        } else {
            log.error("消息没有到达交换机：{}", cause);
        }
    }

    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.error("消息没有到达队列，交换机：{}，路由键：{}，消息内容：{}", exchange, routingKey, message.toString());
    }

    // 1. 业务交换机：普通的交换机 使用已有的：ORDER_EXCHANGE

    // 2. 延迟队列：延迟时间  死信交换机  死信的rk 。延迟队列不能有消费者
    @Bean
    public Queue ttlQueue(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl", 120000);
        arguments.put("x-dead-letter-exchange", "ORDER_EXCHANGE");
        arguments.put("x-dead-letter-routing-key", "stock.unlock");
        return new Queue("WMS_TTL_QUEUE", true, false, false, arguments);
    }

    // 3. 延迟队列绑定到业务交换机
    @Bean
    public Binding ttlBinding(){
        return new Binding("WMS_TTL_QUEUE", Binding.DestinationType.QUEUE, "ORDER_EXCHANGE", "stock.ttl", null);
    }

    // 4. 死信交换机：普通交换机（ORDER_EXCHANGE）

    // 5. 死信队列：普通队列（ORDER_STOCK_QUEUE）

    // 6. 死信队列绑定到死信交换机(已绑定)
}
