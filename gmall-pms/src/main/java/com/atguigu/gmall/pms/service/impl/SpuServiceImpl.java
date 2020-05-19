package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.util.CollectionUtils;

import javax.xml.crypto.Data;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuPageByCid(Long categoryId, PageParamVo paramVo) {

        QueryWrapper<SpuEntity> queryWrapper = new QueryWrapper<>();

        // 判断分类id是否为空
        if (categoryId != 0) {
            queryWrapper.eq("category_id", categoryId);
        }

        // 判断关键字是否为空
        String key = paramVo.getKey();
        if (StringUtils.isNotBlank(key)){
            queryWrapper.and(wrapper -> wrapper.eq("id", key).or().like("name", key));
        }

        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                queryWrapper
        );

        return new PageResultVo(page);
    }

    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Override
    public void bigSave(SpuVo spuVo) {
        // 1. 保存spu相关信息
        // 1.1. 保存spu的信息 pms_spu
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
        Long spuId = spuVo.getId();

        // 1.2. 保存spu的描述信息 pms_spu_desc
        List<String> spuImages = spuVo.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)){
            String descript = StringUtils.join(spuImages, ",");
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(descript);
            this.descMapper.insert(spuDescEntity);
        }

        // 1.3. 保存spu的基本属性信息 pms_spu_attr_value
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){

            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(attr -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(attr, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }

        // 2. 保存sku相关信息
        // 2.1. 保存sku的信息 pms_sku

        // 2.2. 保存sku的图片信息 pms_sku_images

        // 2.3. 保存sku的销售属性 pms_sku_attr_value

        // 3. 保存sku的营销信息
        // 3.1. 保存积分优惠 sms_sku_bounds

        // 3.2. 保存满减信息 sms_sku_full_reduction

        // 3.3. 保存打折信息 sms_sku_ladder
    }

}
