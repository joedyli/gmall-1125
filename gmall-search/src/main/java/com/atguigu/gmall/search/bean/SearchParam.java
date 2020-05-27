package com.atguigu.gmall.search.bean;

import lombok.Data;

import java.util.List;

/**
 * search.gmall.com/xxx?keyword=手机&brandId=1,2&cid=225,250&props=5:128G-256G&props=4:8G-12G&sort=3&priceFrom=1000&priceTo=10000&pageNum=1&store=true
 */
@Data
public class SearchParam {

    // 搜索框中的条件
    private String keyword;

    // 品牌过滤条件
    private List<Long> brandId;

    // 分类过滤条件
    private List<Long> cid;

    // 规格参数过滤条件props=5:128G-256G&props=4:8G-12G
    // ["5:128G-256G", "4:8G-12G"]
    private List<String> props;

    private Integer sort; // 默认得分排序，1-价格降序 2-价格升序 3-销量降序 4-新品降序

    // 价格区间的筛选
    private Integer priceFrom;
    private Integer priceTo;

    // 接收分页参数
    private Integer pageNum = 1;
    private final Integer pageSize = 20;

    // 是否有货
    private Boolean store;
}
