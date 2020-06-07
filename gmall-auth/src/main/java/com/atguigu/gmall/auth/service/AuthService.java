package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsFeign;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.exception.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.geom.IllegalPathStateException;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private JwtProperties properties;

    @Autowired
    private GmallUmsFeign umsFeign;

    public void accredit(String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1.调用远程接口查询用户信息
            ResponseVo<UserEntity> userEntityResponseVo = this.umsFeign.queryUser(loginName, password);
            UserEntity userEntity = userEntityResponseVo.getData();

            // 2.判断用户信息是否为空，为空提示用户名或密码错误
            if (userEntity == null){
                throw new UserException("用户名或者密码有误！");
            }

            // 3. 制作jwt信息
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userEntity.getId());
            map.put("userName", userEntity.getUsername());
            map.put("ip", IpUtil.getIpAddressAtService(request));
            String token = JwtUtils.generateToken(map, this.properties.getPrivateKey(), this.properties.getExpire());

            // 4.放入cookie中
            CookieUtils.setCookie(request, response, this.properties.getCookieName(), token, this.properties.getExpire() * 60);
            // 为了展示欢迎您，需要昵称保存到cookie中去，为了将来前端页面可以解析cookie取出nick名称，这里不要设置httpOnly
            CookieUtils.setCookie(request, response, this.properties.getUnick(), userEntity.getNickname(), this.properties.getExpire() * 60);
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserException("服务器错误！");
        }
    }
}
