package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "wms:stock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {

        if (CollectionUtils.isEmpty(lockVos)) {
            return null;
        }

        // 遍历所有的商品进行验库锁库
        lockVos.forEach(lockVo -> {
            checkLock(lockVo);
        });

        // 如果有一个商品锁定失败了，所有的锁定成功的商品要解锁库存。并把锁定情况响应给用户
        boolean flag = lockVos.stream().anyMatch(skuLockVo -> !skuLockVo.getLock());
        if (flag) {
            // 获取锁定成功的商品列表
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            successLockVos.forEach(skuLockVo -> {
                this.wareSkuMapper.unlock(skuLockVo.getWareSkuId(), skuLockVo.getCount());
            });
            return lockVos;
        }

        // 把库存的锁定信息保存到redis中，方便将来减库存
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos), 5, TimeUnit.MINUTES);

        // 发送延时消息，定时解锁库存
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE",  "stock.ttl", orderToken);

        return null;
    }

    private void checkLock(SkuLockVo lockVo){
        RLock fairLock = this.redissonClient.getFairLock("lock:" + lockVo.getSkuId());
        fairLock.lock();

        // 验库存，如果没有仓库满足用户的购买需求，直接锁定失败
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.check(lockVo.getSkuId(), lockVo.getCount());
        if (CollectionUtils.isEmpty(wareSkuEntities)){
            lockVo.setLock(false); // 设置该商品锁定的状态为失败状态
            fairLock.unlock(); // 释放锁
            return;
        }

        // 锁库存。实际需要大数据分析就近选择仓库。这里直接选择第一个仓库
        WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
        if (this.wareSkuMapper.lock(wareSkuEntity.getId(), lockVo.getCount()) == 1) {
            lockVo.setLock(true); // 状态设置为成功状态
            lockVo.setWareSkuId(wareSkuEntity.getId()); // 记录锁定仓库的id
        }
        fairLock.unlock();
    }

}
