package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class indexController {

    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model){

        // 一级分类
        List<CategoryEntity> categoryEntityList = this.indexService.queryLvl1Categories();
        model.addAttribute("categories", categoryEntityList);
        // TODO：板块广告查询
        return "index";
    }

    @GetMapping("/index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLvl2CategoriesWithSubs(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryLvl2CategoriesWithSubs(pid);
        return ResponseVo.ok(categoryEntities);
    }
}
