package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.UserInfo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("confirm")
    public String confirm(Model model){
        OrderConfirmVo confirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo", confirmVo);
        return "trade";
    }

    @PostMapping("submit")
    @ResponseBody
    public ResponseVo<Object> submit(@RequestBody OrderSubmitVo submitVo){
        OrderEntity orderEntity = this.orderService.submit(submitVo);
        if (orderEntity == null){
            return ResponseVo.fail("服务器错误，订单创建失败！");
        }
        return ResponseVo.ok(orderEntity.getOrderSn());
    }

    @GetMapping("sec/kill/{skuId}")
    public ResponseVo<Object> seckill(@PathVariable("skuId")Long skuId){
        // 过滤器已经添加了登录状态的判断，判断在这里可略
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        String countString = this.redisTemplate.opsForValue().get("sec:kill:" + skuId);
        if (StringUtils.isBlank(countString)){
            throw new OrderException("不存在该商品的秒杀信息");
        }
        Integer count = Integer.parseInt(countString);
        RSemaphore semaphore = this.redissonClient.getSemaphore("semaphore:" + skuId);
        semaphore.trySetPermits(count);

        RLock fairLock = this.redissonClient.getFairLock("lock:" + skuId);
        fairLock.lock();

        if (count == 0) {
            throw new OrderException("手慢了，秒杀已结束！");
        }

        // 减库存
        this.redisTemplate.opsForValue().set("sec:kill:" + skuId, String.valueOf(--count));

        // 发送消息创建订单，并真正去减库存（mysql） userId skuid count
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userInfo.getUserId());
        map.put("skuId", skuId);
        map.put("count", 1);

        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("countdown:" + userInfo.getUserId());
        countDownLatch.trySetCount(1);

        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "sec:kill", map);

        fairLock.unlock();

        return ResponseVo.ok();
    }

    @GetMapping("sec/kill/success")
    public ResponseVo<Object> seckillSuccess() throws InterruptedException {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        // 查询改用户秒杀成功的订单信息，方便付款。
        // 由于秒杀订单的创建是异步的，秒杀成功之后立马查询订单去付款。可能出现查询不到的这种情况
        // 为了避免这种情况的出现，可以使用闭锁countdownlatch
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("countdown:" + userId);
        countDownLatch.await();
        // 查询订单，并响应给用户

        // 订单页面
        return ResponseVo.ok();
    }

}
