package com.atguigu.gmall.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByPid(Long pid) {
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();

        if (pid != -1) {
            wrapper.eq("parent_id", pid);
        }

        return this.list(wrapper);
    }

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
        return this.categoryMapper.queryCategoriesWithSubByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByCid3(Long cid3) {

        CategoryEntity lvl3Category = this.getById(cid3);

        Long cid2 = lvl3Category.getParentId();
        CategoryEntity lvl2Category = this.getById(cid2);

        Long cid1 = lvl2Category.getParentId();
        CategoryEntity lvl1Category = this.getById(cid1);

        return Arrays.asList(
                lvl1Category, lvl2Category, lvl3Category
        );
    }

}
