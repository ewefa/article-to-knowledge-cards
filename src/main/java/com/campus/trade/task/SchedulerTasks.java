package com.campus.trade.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.HomeService;
import com.campus.trade.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定时任务：浏览量增量落库、热门商品缓存刷新、过期商品自动下架
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerTasks {

    private final StringRedisTemplate redisTemplate;

    private final ProductMapper productMapper;

    private final HomeService homeService;

    private final ProductService productService;

    /**
     * 每 2 分钟把 Redis 中累积的浏览量增量写回 MySQL。
     * 使用 HGETALL 后逐个 delete，避免并发丢失。
     */
    @Scheduled(fixedDelay = 120_000L, initialDelay = 60_000L)
    public void flushProductViews() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.PRODUCT_VIEW_DELTA);
        if (entries.isEmpty()) {
            return;
        }
        int flushed = 0;
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                Long productId = Long.valueOf(String.valueOf(entry.getKey()));
                long delta = Long.parseLong(String.valueOf(entry.getValue()));
                if (delta > 0) {
                    productMapper.increaseViewCount(productId, delta);
                    flushed++;
                }
            } catch (NumberFormatException e) {
                log.warn("浏览量增量数据格式异常，已跳过：{} -> {}", entry.getKey(), entry.getValue());
            }
            redisTemplate.opsForHash().delete(RedisKeys.PRODUCT_VIEW_DELTA, entry.getKey());
        }
        if (flushed > 0) {
            log.info("浏览量增量落库完成，更新 {} 件商品", flushed);
        }
    }

    /**
     * 每 10 分钟刷新首页热门推荐缓存（Redis）
     */
    @Scheduled(fixedDelay = 600_000L, initialDelay = 120_000L)
    public void refreshHotProducts() {
        productService.evictHomeCache();
        homeService.hot(8);
        log.info("首页热门商品缓存已刷新");
    }

    /**
     * 每天凌晨 3 点自动下架超过发布期限的在售商品
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void offlineExpiredProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .isNotNull(Product::getExpireTime)
                .lt(Product::getExpireTime, LocalDateTime.now());
        List<Product> expired = productMapper.selectList(wrapper);
        if (expired.isEmpty()) {
            return;
        }
        for (Product product : expired) {
            product.setStatus(2);
            productMapper.updateById(product);
        }
        productService.evictHomeCache();
        log.info("已自动下架 {} 件过期商品", expired.size());
    }
}
