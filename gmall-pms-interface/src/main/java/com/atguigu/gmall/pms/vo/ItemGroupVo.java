package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

@Data
public class ItemGroupVo {

    // 规格参数组的名称
    private String groupName;
    // 分组下的规格参数列表
    private List<AttrValueVo> attrValues;
}
