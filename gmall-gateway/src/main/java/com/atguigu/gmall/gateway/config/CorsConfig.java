package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        CorsConfiguration config = new CorsConfiguration();
        // 允许跨域访问的域名。*：代表所有域名跨域跨域访问；不推荐使用* ，使用*无法携带cookie
        config.addAllowedOrigin("http://manager.gmall.com");
        config.addAllowedOrigin("http://www.gmall.com");
        config.addAllowedOrigin("http://gmall.com");
        config.addAllowedOrigin("http://localhost:1000");
        // 允许携带cookie信息
        config.setAllowCredentials(true);
        // 允许所有类型的请求方法跨域访问
        config.addAllowedMethod("*");
        // 允许携带所有的头信息跨域访问
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        // 拦截进入网关服务的所有请求
        configurationSource.registerCorsConfiguration("/**", config);

        // 初始化cor过滤器
        return new CorsWebFilter(configurationSource);
    }
}
