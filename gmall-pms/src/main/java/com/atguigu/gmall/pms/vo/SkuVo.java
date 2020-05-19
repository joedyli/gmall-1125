package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuVo extends SkuEntity {

    // sku图片列表
    private List<String> images;

    // 接收sku的销售属性及值
    private List<SkuAttrValueEntity> saleAttrs;

    // sku的积分优惠信息
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<Integer> work;

    // sku的打折信息
    private Integer fullCount;
    private BigDecimal discount;
    private Integer ladderAddOther;

    // sku的满减信息
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;
}
