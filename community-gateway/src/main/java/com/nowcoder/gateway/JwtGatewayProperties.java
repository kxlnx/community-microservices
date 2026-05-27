package com.nowcoder.gateway;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@Validated
@ConfigurationProperties(prefix = "gateway.jwt")
public class JwtGatewayProperties {

    private static final Logger log = LoggerFactory.getLogger(JwtGatewayProperties.class);

    /** JWT签名密钥 */
    @NotBlank(message = "JWT密钥不能为空")
    private String secret = "community-microservice-secret-key-2024";

    /** JWT过期时间（秒） */
    @Min(value = 60, message = "JWT过期时间不能少于60秒")
    private long expiration = 43200000;

    /** 白名单路径（逗号分隔） */
    @NotBlank(message = "JWT白名单不能为空")
    private String whitelist = "/login,/register,/kaptcha,/activation,/search,/index,/discuss,/css,/js,/img,/fonts";

    @PostConstruct
    public void validate() {
        if (secret.length() < 16) {
            log.warn("JWT密钥长度不足(当前{}位，建议>=16位)，存在安全风险", secret.length());
        }
        log.info("JWT配置加载完成: secret长度={}, 过期时间={}秒, 白名单={}", secret.length(), expiration, whitelist.length());
    }

    /** 存放JWT的Cookie名 */
    private String cookieName = "token";

    /** 转发的用户ID Header名 */
    private String userIdHeader = "X-User-Id";

    /** 转发的权限 Header名 */
    private String authorityHeader = "X-Authority";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getExpiration() { return expiration; }
    public void setExpiration(long expiration) { this.expiration = expiration; }
    public String getWhitelist() { return whitelist; }
    public void setWhitelist(String whitelist) { this.whitelist = whitelist; }
    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    public String getUserIdHeader() { return userIdHeader; }
    public void setUserIdHeader(String userIdHeader) { this.userIdHeader = userIdHeader; }
    public String getAuthorityHeader() { return authorityHeader; }
    public void setAuthorityHeader(String authorityHeader) { this.authorityHeader = authorityHeader; }

    public List<String> getWhitelistAsList() {
        return List.of(whitelist.split(","));
    }
}
