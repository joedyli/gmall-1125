package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Time;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cats:";

    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0l);
        return listResponseVo.getData();
    }

    @GmallCache(prefix = KEY_PREFIX, lock = "lock", timeout = 129600l, random = 10080)
    public List<CategoryEntity> queryLvl2CategoriesWithSubs(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();

        return categoryEntities;
    }

    public List<CategoryEntity> queryLvl2CategoriesWithSubs2(Long pid) {
        // 1.查询缓存 有则直接命中返回
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseArray(json, CategoryEntity.class);
        }

        // 为了防止缓存击穿，在这里添加分布式锁
        RLock fairLock = this.redissonClient.getFairLock("lock" + pid);
        fairLock.lock();
        // 再次查询缓存：1.加锁的过程中可能有其他请求放入缓存
        // 2.高并发情况下，第一个请求获取到锁之后，发送远程请求查询数据并放入缓存。其他请求就不用发送请求
        String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)){
            fairLock.unlock();
            return JSON.parseArray(json2, CategoryEntity.class);
        }

        // 2.没有，则查询数据库
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        // 放入缓存，
        // 1.为避免缓存穿透，这里不要判断数据是否为空（数据即使为空也放入缓存）
        // 2.为了避免缓存的雪崩，应该给缓存时间添加一个随机值
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 3 * 30 + new Random().nextInt(7), TimeUnit.DAYS);

        fairLock.unlock();

        return categoryEntities;
    }

    public void testLock() {
        // 加锁
        RLock lock = this.redissonClient.getLock("lock");
//        lock.lock(20, TimeUnit.SECONDS);
        lock.lock();

        // 读取redis中的num值
        String numString = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(numString)){
            return;
        }

        // ++操作
        Integer num = Integer.parseInt(numString);
        num++;

        // 放入redis
        this.redisTemplate.opsForValue().set("num", String.valueOf(num));

        // 释放锁
        lock.unlock();
    }

    public void testLock2() {
        // 执行setnx指令
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        // 返回值为true，执行业务逻辑
        if (lock){
//            this.redisTemplate.expire("lock", 3, TimeUnit.SECONDS);
            // 读取redis中的num值
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)){
                return;
            }

            // ++操作
            Integer num = Integer.parseInt(numString);
            num++;

            // 放入redis
            this.redisTemplate.opsForValue().set("num", String.valueOf(num));

            // 删除锁。为了防止误删，需要判断这个锁是不是自己的
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then return redis.call('del', KEYS[1]) " +
                    "else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script), Arrays.asList("lock"), uuid);
//            if (StringUtils.equals(this.redisTemplate.opsForValue().get("lock"), uuid)){
//                this.redisTemplate.delete("lock");
//            }
        } else {
            // 返回值false，则重试
            try {
                Thread.sleep(200);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String testRead() {
        // 加读锁
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10, TimeUnit.SECONDS);

        // 模拟业务逻辑 TODO

//        rwLock.readLock().unlock();

        return "测试读锁！";
    }

    public String testWrite() {

        // 加读锁
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);

        // 模拟业务逻辑 TODO

//        rwLock.writeLock().unlock();

        return "测试写锁！";
    }

    public String testLatch() throws InterruptedException {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.trySetCount(6);

        latch.await();
        return "班长锁门！！";
    }

    public String testCountDown() {
        RCountDownLatch latch = this.redissonClient.getCountDownLatch("latch");
        latch.countDown();
        return "出来了一位同学！";
    }
}
