package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVo {

    private Long skuId;
    private Integer count;
    private Boolean lock; // 锁定状态，true-锁定成功 false-锁定失败
    private Long wareSkuId; // 锁定成功的仓库id
}
