package com.atguigu.gmall.search.bean;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {

    // 品牌过滤
    private List<BrandEntity> brands;

    // 分类过滤
    private List<CategoryEntity> categories;

    // 规格参数
    private List<SearchResposneAttrVo> filters;

    // 分页数据
    private Integer pageNum;
    private Integer pageSize;
    private Long total;

    // 当前页的数据
    private List<Goods> goodsList;
}
