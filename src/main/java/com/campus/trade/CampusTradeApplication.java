package com.campus.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手交易平台 - 启动类
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan("com.campus.trade.config")
@MapperScan("com.campus.trade.mapper")
public class CampusTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusTradeApplication.class, args);
    }
}
