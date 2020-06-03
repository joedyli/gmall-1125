package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private SkuFullReductionMapper reductionMapper;

    @Autowired
    private SkuLadderMapper ladderMapper;

    @Override
    @Transactional
    public void saveSkuSales(SkuSaleVo skuSaleVo) {
        // 3.1. 保存积分优惠 sms_sku_bounds
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        skuBoundsEntity.setSkuId(skuSaleVo.getSkuId());
        skuBoundsEntity.setGrowBounds(skuSaleVo.getGrowBounds());
        skuBoundsEntity.setBuyBounds(skuSaleVo.getBuyBounds());
        List<Integer> works = skuSaleVo.getWork();
        skuBoundsEntity.setWork(works.get(3) * 8 + works.get(2) * 4 + works.get(1) * 2 + works.get(0));
        this.save(skuBoundsEntity);

        // 3.2. 保存满减信息 sms_sku_full_reduction
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        reductionEntity.setSkuId(skuSaleVo.getSkuId());
        reductionEntity.setFullPrice(skuSaleVo.getFullPrice());
        reductionEntity.setReducePrice(skuSaleVo.getReducePrice());
        reductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        this.reductionMapper.insert(reductionEntity);

        // 3.3. 保存打折信息 sms_sku_ladder
        SkuLadderEntity ladderEntity = new SkuLadderEntity();
        ladderEntity.setSkuId(skuSaleVo.getSkuId());
        ladderEntity.setFullCount(skuSaleVo.getFullCount());
        ladderEntity.setDiscount(skuSaleVo.getDiscount());
        ladderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        this.ladderMapper.insert(ladderEntity);
    }

    @Override
    public List<ItemSaleVo> querySaleVosBySkuId(Long skuId) {

        List<ItemSaleVo> itemSaleVos = new ArrayList<>();

        // 积分
        SkuBoundsEntity boundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (boundsEntity != null) {
            ItemSaleVo boundSaleVo = new ItemSaleVo();
            boundSaleVo.setType("积分");
            boundSaleVo.setDesc("送" + boundsEntity.getGrowBounds() + "成长积分，送" + boundsEntity.getBuyBounds() + "购物积分");
            itemSaleVos.add(boundSaleVo);
        }

        // 满减
        SkuFullReductionEntity reductionEntity = this.reductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (reductionEntity != null) {
            ItemSaleVo reductionSaleVo = new ItemSaleVo();
            reductionSaleVo.setType("满减");
            reductionSaleVo.setDesc("满" + reductionEntity.getFullPrice() + "减" + reductionEntity.getReducePrice());
            itemSaleVos.add(reductionSaleVo);
        }

        // 打折
        SkuLadderEntity ladderEntity = this.ladderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (ladderEntity != null) {
            ItemSaleVo ladderSaleVo = new ItemSaleVo();
            ladderSaleVo.setType("打折");
            ladderSaleVo.setDesc("满" + ladderEntity.getFullCount() + "件打" + ladderEntity.getDiscount().divide(new BigDecimal(10)) + "折");
            itemSaleVos.add(ladderSaleVo);
        }

        return itemSaleVos;
    }

}
