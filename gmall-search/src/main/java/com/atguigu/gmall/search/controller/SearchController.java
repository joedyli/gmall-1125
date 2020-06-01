package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.bean.SearchParam;
import com.atguigu.gmall.search.bean.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("search")
    public String search(SearchParam searchParam, Model model){

        SearchResponseVo responseVo = this.searchService.search(searchParam);
        model.addAttribute("response", responseVo);
        model.addAttribute("searchParam", searchParam);
        return "search";
    }
}
