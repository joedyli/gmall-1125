package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.bean.Goods;
import com.atguigu.gmall.search.bean.SearchAttrVo;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class SearchTest {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Test
    public void test(){

    }

    @Test
    public void importData(){

        this.restTemplate.createIndex(Goods.class);
        this.restTemplate.putMapping(Goods.class);

        // 定义分页参数
        Integer pageNum = 1;
        Integer pageSize = 100;

        do{
            // 分批获取spu
            ResponseVo<List<SpuEntity>> spuResponseVo = this.pmsClient.querySpuPage(new PageParamVo(pageNum, pageSize, null));
            List<SpuEntity> spuEntities = spuResponseVo.getData();
            // 如果最后一页刚好是100条记录，会再查询一次，但是没有spu数据
            if (CollectionUtils.isEmpty(spuEntities)){
                return;
            }
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkusBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)){
                    // 把List<SkuEntity> --> List<Goods>
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();

                        // sku的基本信息
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setPrice(skuEntity.getPrice());

                        // 创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());

                        // 品牌
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(spuEntity.getBrandId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        // 分类
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(spuEntity.getCategoryId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                        // 销量和库存
                        ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            // 只要有一个仓库的库存余额 - 锁定库存的数量 > 0
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                            // 销量，获取所有仓库的销量之和
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                        }

                        List<SearchAttrVo> searchAttrs = new ArrayList<>();

                        // 通用属性（检索参数）
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrResponseVo = this.pmsClient.querySpuAttrValuesBySpuId(spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrResponseVo.getData();
                        if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            searchAttrs.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrVo searchAttrVo = new SearchAttrVo();
                                BeanUtils.copyProperties(spuAttrValueEntity, searchAttrVo);
                                return searchAttrVo;
                            }).collect(Collectors.toList()));
                        }

                        // 销售属性（检索参数）
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySkuAttrValuesBySkuId(skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            searchAttrs.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrVo searchAttrVo = new SearchAttrVo();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrVo);
                                return searchAttrVo;
                            }).collect(Collectors.toList()));
                        }

                        goods.setSearchAttrs(searchAttrs);

                        return goods;
                    }).collect(Collectors.toList());

                    // 执行批量插入索引的操作
                    this.goodsRepository.saveAll(goodsList);
                }
            });

            pageNum++;
            // 获取当前页的记录数
            pageSize = spuEntities.size();
        }while(pageSize == 100);
    }
}
