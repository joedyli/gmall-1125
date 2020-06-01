package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "index:cats:";

    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0l);
        return listResponseVo.getData();
    }

    public List<CategoryEntity> queryLvl2CategoriesWithSubs(Long pid) {
        // 1.查询缓存 有则直接命中返回
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)){
            return JSON.parseArray(json, CategoryEntity.class);
        }

        // 2.没有，则查询数据库
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        // 放入缓存
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities));

        return categoryEntities;
    }
}
