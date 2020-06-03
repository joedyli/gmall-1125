package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public List<SkuAttrValueEntity> querySkuAttrValuesBySkuId(Long skuId) {
        return skuAttrValueMapper.querySkuAttrValuesBySkuId(skuId);
    }

    @Override
    public List<SaleAttrValueVo> querySaleAttrValuesBySpuId(Long spuId) {
        List<SkuAttrValueEntity> attrValueEntities = this.skuAttrValueMapper.querySaleAttrValuesBySpuId(spuId);

        // 对集合以attrId进行分组，每个分组就对应一个SaleAttrValueVo。返回值是一个map，key-attrId value-
        Map<Long, List<SkuAttrValueEntity>> map = attrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));

        // 初始化方法需要的集合
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        // 遍历map，每一个map对应SaleAttrValueVo
        map.forEach((attrId, skuAttrValueEntities) -> {
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
                saleAttrValueVo.setAttrId(attrId);
                saleAttrValueVo.setAttrName(skuAttrValueEntities.get(0).getAttrName());
                saleAttrValueVo.setAttrValues(skuAttrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
                saleAttrValueVos.add(saleAttrValueVo);
            }
        });
        return saleAttrValueVos;
    }

    @Override
    public String querySkuJsonsBySpuId(Long spuId) {
        // [{sku_id: 1, attr_values: '黑色，8G, 128G'}, {sku_id: 1, attr_values: '黑色，8G, 128G'}]
        List<Map<String, Object>> maps = this.skuAttrValueMapper.querySkuJsonsBySpuId(spuId);
        // {'黑色, 8G, 256G': 30, '黑色, 12G, 512G': 32}
        Map<String, Long> attrValuesSkuIdMap =
                maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long) map.get("sku_id")));
        // 把map结构的数据序列化为json字符串输出
        return JSON.toJSONString(attrValuesSkuIdMap);
    }

}
