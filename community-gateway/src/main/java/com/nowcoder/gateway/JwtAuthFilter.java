package com.nowcoder.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtGatewayProperties props;
    private List<String> whitelist;

    public JwtAuthFilter(JwtGatewayProperties props) {
        this.props = props;
        this.whitelist = props.getWhitelistAsList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 静态资源直接放行
        if (isStaticResource(path)) {
            return chain.filter(exchange);
        }

        // 提取 JWT
        String token = extractToken(exchange);

        // 注入用户 Header
        if (token != null) {
            exchange = injectUserHeaders(exchange, token);
        }

        // 白名单放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 非白名单需要鉴权
        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    /** 从 Cookie 或 Authorization Header 中提取 JWT token */
    private String extractToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        List<HttpCookie> cookies = exchange.getRequest().getCookies().get(props.getCookieName());
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.get(0).getValue();
        }
        return null;
    }

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /** 验证 JWT 并将 userId/authority 注入请求 Header */
    private ServerWebExchange injectUserHeaders(ServerWebExchange exchange, String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            String userId = claims.getSubject();
            String authority = claims.get("authority", String.class);
            return exchange.mutate()
                    .request(r -> r.header(props.getUserIdHeader(), userId)
                            .header(props.getAuthorityHeader(), authority))
                    .build();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
            return exchange;
        } catch (MalformedJwtException e) {
            log.warn("JWT 格式错误: {}", e.getMessage());
            return exchange;
        } catch (SignatureException e) {
            log.warn("JWT 签名无效: {}", e.getMessage());
            return exchange;
        } catch (Exception e) {
            log.error("JWT 解析异常: {}", e.getMessage());
            return exchange;
        }
    }

    /** 判断路径是否在白名单中 */
    private boolean isWhitelisted(String path) {
        if ("/".equals(path)) return true;
        for (String wl : whitelist) {
            if (path.startsWith(wl)) return true;
        }
        return false;
    }

    private boolean isStaticResource(String path) {
        return path.matches(".*\\.(css|js|png|jpg|jpeg|ico|html)$");
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
