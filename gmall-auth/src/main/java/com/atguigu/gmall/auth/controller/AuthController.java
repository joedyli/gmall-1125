package com.atguigu.gmall.auth.controller;

import com.atguigu.gmall.auth.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/toLogin.html")
    public String toLogin(@RequestParam(value = "returnUrl", required = false)String returnUrl, Model model){
        model.addAttribute("returnUrl", returnUrl);
        return "login";
    }

    @PostMapping("login")
    public String login(@RequestParam(value = "returnUrl", required = false)String returnUrl,
                        @RequestParam(value = "loginName")String loginName,
                        @RequestParam(value = "password")String password,
                        HttpServletRequest request,
                        HttpServletResponse response
                        ){
        this.authService.accredit(loginName, password, request, response);
        // 判断returnUrl是否为空，为空重定向到首页
        returnUrl = StringUtils.isNotBlank(returnUrl) ? returnUrl : "http://www.gmall.com";
        return "redirect:" + returnUrl;
    }

}
