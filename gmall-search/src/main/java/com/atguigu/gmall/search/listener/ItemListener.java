package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.bean.Goods;
import com.atguigu.gmall.search.bean.SearchAttrVo;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ItemListener {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "GMALL_SEARCH_QUEUE", durable = "true"),
            exchange = @Exchange(value = "GMALL_ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.insert", "item.update"}
    ))
    public void listen(Long spuId, Channel channel, Message message) throws IOException {
        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkusBySpuId(spuId);
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

                // 根据spuid查询spu
                ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
                SpuEntity spuEntity = spuEntityResponseVo.getData();
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
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
