package com.campus.trade.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台自定义配置，对应 application.yml 中的 campus 节点
 */
@Data
@ConfigurationProperties(prefix = "campus")
public class CampusProperties {

    private Jwt jwt = new Jwt();

    private Upload upload = new Upload();

    private Product product = new Product();

    private Sms sms = new Sms();

    @Data
    public static class Jwt {

        /** 签名密钥（至少 32 字节）；留空则由 JwtUtil 在启动时随机生成，仅适合本地开发 */
        private String secret;

        /** 令牌有效期（小时） */
        private long expireHours = 24;
    }

    @Data
    public static class Upload {

        /** 上传文件保存目录 */
        private String dir = "./uploads";

        /** 访问前缀 */
        private String urlPrefix = "/uploads";
    }

    @Data
    public static class Product {

        /** 发布商品是否需要审核（数据库配置优先，此处为默认值） */
        private boolean auditEnabled = false;

        /** 上架多少天后自动下架 */
        private int expireDays = 30;
    }

    @Data
    public static class Sms {

        /** 演示模式：不真正发送短信，接口直接返回验证码，方便本地体验 */
        private boolean mock = true;
    }
}
