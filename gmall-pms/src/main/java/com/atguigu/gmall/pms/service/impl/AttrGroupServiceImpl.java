package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public List<GroupVo> queryGroupsWithAttrsByCid(Long cid) {
        // 1.查询规格参数分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(groupEntities)) {
            return null;
        }

        // 2.遍历分组，查询每个组下的规格参数
        return groupEntities.stream().map(attrGroupEntity -> {
            GroupVo groupVo = new GroupVo();
            BeanUtils.copyProperties(attrGroupEntity, groupVo);

            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("type", 1).eq("group_id", attrGroupEntity.getId()));
            groupVo.setAttrEntities(attrEntities);
            return groupVo;
        }).collect(Collectors.toList());
    }

    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public List<ItemGroupVo> queryItemGroupVoByCidAndSpuIdAndSkuId(Long cid, Long spuId, Long skuId) {
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(attrGroupEntities)){
            return null;
        }

        return attrGroupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            // 设置分组名称
            itemGroupVo.setGroupName(attrGroupEntity.getName());

            List<AttrValueVo> attrValueVos = new ArrayList<>();

            // 查询当前组下的基本属性及属性值
            List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.querySpuAttrValuesBySpuIdAndGId(spuId, attrGroupEntity.getId());
            if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList()));
            }

            // 查询当前组下的销售属性及属性值
            List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.querySkuAttrValuesBySkuIdAndGid(skuId, attrGroupEntity.getId());
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList()));
            }

            itemGroupVo.setAttrValues(attrValueVos);
            return itemGroupVo;
        }).collect(Collectors.toList());
    }

//    public static void main(String[] args) {
//        List<User> users = Arrays.asList(
//                new User(1l, "柳岩", 20),
//                new User(2l, "小鹿", 21),
//                new User(3l, "马蓉", 22),
//                new User(4l, "冰冰", 23),
//                new User(5l, "柏芝", 24),
//                new User(6l, "马苏", 25)
//        );
//
//        // map
//        List<String> names = users.stream().map(User::getName).collect(Collectors.toList());
//        System.out.println(names);
//
//        // filter
//        users.stream().filter(user -> user.getAge() > 22).collect(Collectors.toList()).forEach(System.out::println);
//        users.stream().filter(user -> user.getAge() % 2 == 0).collect(Collectors.toList()).forEach(System.out::println);
//
//        // reduce
//        System.out.println(users.stream().map(User::getAge).reduce((a, b) -> a + b).get());
//    }

}

//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//class User {
//    private Long id;
//    private String name;
//    private Integer age;
//}
