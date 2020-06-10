package com.atguigu.gmall.order.interceptor;

import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.order.vo.UserInfo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
//@Scope("prototype") // singleton prototype：多例模式 request session globalSession
public class LoginInterceptor implements HandlerInterceptor {

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

        // 通过请求头信息获取登录信息（网关过滤器已经校验了登录状态，并且通过头信息传递给订单服务）
        String userId = request.getHeader("userId");
        if (StringUtils.isNotBlank(userId)){
            UserInfo userInfo = new UserInfo(Long.valueOf(userId), null);
            THREAD_LOCAL.set(userInfo);
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
    }
}
