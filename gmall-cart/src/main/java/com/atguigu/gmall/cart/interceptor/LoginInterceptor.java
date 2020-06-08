package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.bean.UserInfo;
import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.catalina.User;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(JwtProperties.class)
//@Scope("prototype") // singleton prototype：多例模式 request session globalSession
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 前置方法：在Handler方法执行之前执行
     * 返回值：true-放行 false-被拦截
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("前置方法正在执行");

        // 获取用户的登录信息（GMALL-TOKEN userKey）
        String token = CookieUtils.getCookieValue(request, this.properties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request, this.properties.getUserKey());
        // 判断userKey是否为空，如果为空生成一个并放入cookie中
        if (StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, properties.getUserKey(), userKey, 15552000);

        }

        // 如果token为空，说明该用户没有登录，传递userKey
        if (StringUtils.isBlank(token)){
            THREAD_LOCAL.set(new UserInfo(null, userKey));
            return true;
        }

        // 从token中解析用户的登录信息
        try {
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());
            THREAD_LOCAL.set(new UserInfo(Long.valueOf(map.get("userId").toString()), userKey));
        } catch (Exception e) {
            THREAD_LOCAL.set(new UserInfo(null, userKey));
            e.printStackTrace();
        }

        // 购物车不管有没有登录都要放行
        return true;
    }

    /**
     * 可以通过静态方法提供获取threadlocal中数据
     * @return
     */
    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    /**
     * 后置方法：在handler方法执行之后执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("后置方法正在执行");
    }

    /**
     * 完成方法：在视图渲染完成之后执行
     * 释放资源
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 一定要手动清除threadLocal中的局部变量。因为我们使用的是tomcat线程池。否则会出现内存泄漏
        THREAD_LOCAL.remove();
        System.out.println("完成方法正在执行");
    }
}
