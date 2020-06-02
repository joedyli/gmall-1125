package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect // 声明该类是一个切面类
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 常见的切点表达式：execution(* com.atguigu.gmall.index.service.*.*(..))
     * 切带指定注解的方法：@annotation(com.atguigu.gmall.index.config.GmallCache)
     * 获取目标方法的参数列表：joinPoint.getArgs()
     * 获取目标方法所在的类：joinPoint.getTarget().getClass();
     * 获取方法签名：(MethodSignature)joinPoint.getSignature();
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        // 获取目标方法的对象
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取目标方法上的gmallCache对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 获取目标方法的返回值类型
        Class returnType = signature.getReturnType();
        // 获取缓存的前缀
        String prefix = gmallCache.prefix();
        String key = prefix + Arrays.asList(joinPoint.getArgs());

        // 先查询缓存
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, returnType);
        }

        // 加分布式锁
        String lock = gmallCache.lock();
        RLock fairLock = this.redissonClient.getFairLock(lock + ":" + Arrays.asList(joinPoint.getArgs()));
        fairLock.lock();

        // 再查缓存
        String json2 = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json2)){
            fairLock.unlock();
            return JSON.parseObject(json2, returnType);
        }

        // 执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        // 放入缓存并释放锁
        long timeout = gmallCache.timeout();
        int random = gmallCache.random();
        Long time = timeout + new Random().nextInt(random);
        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), time, TimeUnit.MINUTES);

        fairLock.unlock();

        return result;
    }
}
