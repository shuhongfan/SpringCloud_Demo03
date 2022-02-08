package cn.itcast.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(-1) //指定过滤器顺序
@Component
public class AuthorizeFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        1.获取请求参数
        ServerHttpRequest request = exchange.getRequest();
        MultiValueMap<String, String> params = request.getQueryParams();
//        2.获取参数中的authorization参数
        String auth = params.getFirst("authorization");
//        3.判断参数值是否等于admin
        //http://localhost:10010/order/101?authorization=admin
        if ("admin".equalsIgnoreCase(auth)){
//        4.是，放行
            return chain.filter(exchange);
        }
//        5.否，拦截
//        5.1设置状态码
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//        5.2拦截请求
        return exchange.getResponse().setComplete();
    }
}
