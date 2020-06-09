package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.cart.bean.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private CartAsyncService cartAsyncService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:info:";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void addCart(Cart cart) {
        // 1.获取用户的登录状态信息
        String userId = getUserId();
        String key = KEY_PREFIX + userId;

        try {
            // 2.获取用户的购物车，hashOps相当于内层的map
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

            // 3.判断用户的购物车中是否包含当前记录。注意：redis中所有泛型都是字符串
            String skuId = cart.getSkuId().toString();
            BigDecimal count = cart.getCount();  // 用户加入购物车时的数量
            if (hashOps.hasKey(skuId)) {
                // 包含，更新数量
                String cartJson = hashOps.get(skuId).toString();
                cart = MAPPER.readValue(cartJson, Cart.class);
                cart.setCount(cart.getCount().add(count));
                // 更新数据库中购物车
                this.cartAsyncService.updateCartByUserIdAndSkuId(cart, userId);
            } else {
                // 不包含，新增记录
                cart.setUserId(userId);
                ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if (skuEntity == null){
                    return;
                }
                cart.setPrice(skuEntity.getPrice());
                cart.setTitle(skuEntity.getTitle());
                cart.setDefaultImage(skuEntity.getDefaultImage());
                cart.setCheck(true);
                // 获取商品库存信息
                ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
                List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)){
                    cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStockLocked() - wareSkuEntity.getStockLocked() > 0));
                }
                // 销售属性
                ResponseVo<List<SkuAttrValueEntity>> SkuAttrValueResponseVo = this.pmsClient.querySaleAttrValuesBySkuId(cart.getSkuId());
                List<SkuAttrValueEntity> skuAttrValueEntities = SkuAttrValueResponseVo.getData();
                cart.setSaleAttrs(MAPPER.writeValueAsString(skuAttrValueEntities));
                // 营销信息
                ResponseVo<List<ItemSaleVo>> saleResponseVo = this.smsClient.querySaleVosBySkuId(cart.getSkuId());
                List<ItemSaleVo> itemSaleVos = saleResponseVo.getData();
                cart.setSales(MAPPER.writeValueAsString(itemSaleVos));

                // 保存到mysql + redis
                this.cartAsyncService.saveCart(cart);
            }
            // 更新redis中购物车
            hashOps.put(skuId, MAPPER.writeValueAsString(cart));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户登录信息的方法
     * @return
     */
    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = "";
        Long uid = userInfo.getUserId();
        if (uid == null) {
            userId = userInfo.getUserKey();
        } else {
            userId = uid.toString();
        }
        return userId;
    }

    public Cart queryCartBySkuId(Long skuId) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        try {
            // 获取该用户的所有购物车
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

            // 判断有没有该商品对应的购物车信息
            if (hashOps.hasKey(skuId.toString())) {
                String cartJson = hashOps.get(skuId.toString()).toString();
                return MAPPER.readValue(cartJson, Cart.class);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        // 防止thymeleaf中出现空指针异常
        return new Cart();
    }

    public List<Cart> queryCarts() {

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        // 1. 查询未登录的购物车
        String unloginKey = KEY_PREFIX + userKey;
        // 获取了未登录的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(unloginKey);
        // 获取未登录购物车的json集合
        List<Object> cartJsons = hashOps.values();
        List<Cart> unloginCarts = null;
        // 反序列化为cart集合
        if (!CollectionUtils.isEmpty(cartJsons)){
            unloginCarts = cartJsons.stream().map(cartJson -> {
                try {
                    return MAPPER.readValue(cartJson.toString(), Cart.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
        }

        // 2. 判断是否登录，未登录直接返回
        Long userId = userInfo.getUserId();
        if (userId == null) {
            return unloginCarts;
        }

        // 3.合并购物车
        String loginKey = KEY_PREFIX + userId;
        // 获取了登录状态购物车操作对象
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        // 判断是否存在未登录的购物车，有则遍历未登录的购物车合并到已登录的购物车中去
        if (!CollectionUtils.isEmpty(unloginCarts)){
            unloginCarts.forEach(cart -> {
                try {
                    // 登录状态购物车已存在该商品，更新数量
                    if (loginHashOps.hasKey(cart.getSkuId().toString())) {
                        // 未登录购物车当前商品的数量
                        BigDecimal count = cart.getCount();
                        // 获取登录状态的购物车并反序列化
                        String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                        cart = MAPPER.readValue(cartJson, Cart.class);
                        // 更新登录状态的购物车
                        cart.setCount(cart.getCount().add(count));
                        this.cartAsyncService.updateCartByUserIdAndSkuId(cart, userId.toString());
                    } else {
                        // 登录状态购物车不包含该记录，新增
                        cart.setUserId(userId.toString()); // 用userId覆盖掉userKey
                        this.cartAsyncService.saveCart(cart);
                    }
                    loginHashOps.put(cart.getSkuId().toString(), MAPPER.writeValueAsString(cart));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
            // 合并完未登录的购物车之后，要删除未登录的购物车
            this.cartAsyncService.deleteCartByUserId(userKey);
            this.redisTemplate.delete(unloginKey);
        }

        // 4.查询登录状态所有购物车信息，反序列化后返回
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(loginCartJson -> {
                try {
                    return MAPPER.readValue(loginCartJson.toString(), Cart.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Async
    public ListenableFuture<String> executor1(){
        try {
            System.out.println("executor1方法开始执行。");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor1方法结束执行。。。。。。" + Thread.currentThread().getName());
            int i = 1/0;
            return AsyncResult.forValue("hello executor1");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e);
        }
    }

    @Async
    public String executor2(){
        try {
            System.out.println("executor2方法开始执行。");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor2方法结束执行。。。。。。" + Thread.currentThread().getName());
            int i = 1/0;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "hello executor2";
    }

//    @Scheduled(fixedRate = 10000)
    public void testScheduled(){
        System.out.println("这是一个定时任务：" + System.currentTimeMillis());
    }


}
