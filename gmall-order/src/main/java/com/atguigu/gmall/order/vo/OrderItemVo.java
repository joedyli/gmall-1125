package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;
    private String defaultImage;
    private String title;
    private List<SkuAttrValueEntity> saleAttrs; // 销售属性：List<SkuAttrValueEntity>
    private BigDecimal price; // 实时价格
    private BigDecimal count;
    private Boolean store = false; // 是否有货
    private List<ItemSaleVo> sales; // 营销信息
    private BigDecimal weight;
}
