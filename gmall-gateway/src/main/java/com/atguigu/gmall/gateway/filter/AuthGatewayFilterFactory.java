package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties properties;

    /**
     * 通过构造方法，告诉父类通过PathConfig接收配置的参数信息
     */
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("这是局部过滤器。。。。。。。。" + config);

                // 获取request对象 response对象 HttpServletRequest(servlet)  -->  ServerHttpRequest(webflux)
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

                // 1.判断路径要不要拦截（当前路径在不在拦截名单中，如果不在拦截名单中直接放行）
                List<String> authPaths = config.getAuthPaths(); // 获取拦截名单 index/cates
                String path = request.getURI().getPath(); // 获取当前请求路径  index/cates/1
                // 如果当前路径不在拦截名单中，放行
                if (authPaths.stream().allMatch(authPath -> path.indexOf(authPath) == -1)) {
                    return chain.filter(exchange);
                }

                // 2.获取token信息（同步请求token信息通过cookie传递，异步请求通过头信息传递token）
                String token = request.getHeaders().getFirst("token"); // 先尝试从头信息获取，异步
                if (StringUtils.isBlank(token)){ // 如果头信息获取不到，再次从cookie中尝试获取
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(properties.getCookieName())){
                        token = cookies.getFirst(properties.getCookieName()).getValue();
                    }
                }

                // 3.判断token是否为空，为空直接重定向到登陆页面
                if (StringUtils.isBlank(token)){
                    // 303状态码，代表重定向，重定向的地址取决于location这个头信息
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI().toString());
                    return response.setComplete();
                }

                try {
                    // 4.尝试解析token信息，出现异常直接重定向登录页面
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());

                    // 5.根据token中的ip和当前请求ip是否符合（防止token的盗取），如果不一样直接重定向登录
                    String ip = map.get("ip").toString(); // 获取token中的ip信息
                    String currentIp = IpUtil.getIpAddressAtGateway(request);
                    if (!StringUtils.equals(ip, currentIp)){
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI().toString());
                        return response.setComplete();
                    }

                    // 6.把登录信息传递给后续的微服务（避免重复解析jwt带来的性能问题）
                    request.mutate().header("userId", map.get("userId").toString()).build();
                    exchange.mutate().request(request).build();
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI().toString());
                    return response.setComplete();
                }

                // 7.放行
                return chain.filter(exchange);
            }
        };
    }

    /**
     * 指定数据模型接收参数的字段顺序
     * @return
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("authPaths");
    }

    /**
     * 指定接收配置参数的字段类型
     * @return
     */
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    /**
     * 定义接收过滤器参数的数据模型
     */
    @Data
    public static class PathConfig{
        private List<String> authPaths;
    }
}
