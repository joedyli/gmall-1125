package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 面包屑所需要的三级分类  V
    private List<CategoryEntity> categories;

    // 面包屑所需要的品牌信息 V
    private Long brandId;
    private String brandName;

    // 面包屑所需要的spu信息 V
    private Long spuId;
    private String spuName;

    // 中间模块：sku一些信息 V
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;
    private String defaultImage;

    // 中间模块：sku的图片列表 V
    private List<SkuImagesEntity> images;

    // 中间模块：sku的促销信息 V
    private List<ItemSaleVo> sales;

    // 中间模块：sku的库存信息 V
    private Boolean store = false;

    // 中间模块：spu所有的销售属性组合 V
    // [{attrId: 8, attrName: 颜色, attrValues: 黑色, 白色, 粉色}
    //	{attrId: 9, attrName: 内存, attrValues: 8G, 12G}
    //	{attrId: 10, attrName: 存储, attrValues: 256G, 512G}]
    private List<SaleAttrValueVo> saleAttrs;
    // 中间模块：当前sku的销售属性 V
    // {8: 黑色, 9: 12G, 10: 256G}
    private Map<Long, String> saleAttr;
    // 销售属性组合 和 skuId的对应关系 V
    // {'黑色, 8G, 256G': 30, '黑色, 12G, 512G': 32}
    private String skuJsons;

    // 商品详情：spu的图片列表 V
    private List<String> spuImages;

    // 规格参数组及组下的规格参数和值
    private List<ItemGroupVo> groups;
}
