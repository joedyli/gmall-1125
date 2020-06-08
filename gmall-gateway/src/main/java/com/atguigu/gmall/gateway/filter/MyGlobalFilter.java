package com.atguigu.gmall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("进入全局过滤器，无差别拦截所有经过网关的请求！");

        // 拦截请求
//        ServerHttpResponse response = exchange.getResponse();
//        response.setStatusCode(HttpStatus.UNAUTHORIZED); // 设置响应状态码
//        return response.setComplete();
        // 放行
        return chain.filter(exchange);
    }

    /**
     * 定义过滤器的执行顺村，数字越小执行优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
