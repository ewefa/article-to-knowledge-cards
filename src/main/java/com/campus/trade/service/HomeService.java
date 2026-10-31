package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.RedisCache;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Notice;
import com.campus.trade.entity.Product;
import com.campus.trade.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首页聚合数据：轮播、分类、热门推荐（Redis 缓存）、最新发布、搜索热词
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final Duration HOT_TTL = Duration.ofMinutes(10);

    private final ProductService productService;

    private final CategoryService categoryService;

    private final NoticeService noticeService;

    private final RedisCache redisCache;

    private final StringRedisTemplate redisTemplate;

    public List<Notice> banners() {
        return noticeService.banners();
    }

    public List<Category> categories() {
        return categoryService.listEnabled();
    }

    /** 热门推荐：优先读 Redis 缓存，缓存失效时回源数据库 */
    public List<ProductVO> hot(int limit) {
        List<ProductVO> cached = redisCache.getList(RedisKeys.HOT_LIST, ProductVO.class);
        if (cached != null && !cached.isEmpty()) {
            return cached.size() <= limit ? cached : new ArrayList<>(cached.subList(0, limit));
        }
        List<ProductVO> list = productService.hot(limit);
        redisCache.set(RedisKeys.HOT_LIST, list, HOT_TTL);
        return list;
    }

    /** 最新发布 */
    public List<ProductVO> latest(int limit) {
        List<Product> products = productService.list(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getPublishTime)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 20)));
        return productService.toVOList(products, null);
    }

    /** 顶部公告 */
    public List<Notice> notices() {
        return noticeService.notices();
    }

    /** 首页聚合接口，一次返回全部首屏数据 */
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("banners", banners());
        data.put("categories", categories());
        data.put("hot", hot(8));
        data.put("latest", latest(8));
        data.put("notices", notices());
        return data;
    }

    /** 搜索热词（Redis ZSet 统计，取前 N） */
    public List<String> hotKeywords(int limit) {
        Set<String> set = redisTemplate.opsForZSet().reverseRange(RedisKeys.SEARCH_HOT, 0, limit - 1L);
        return set == null ? List.of() : new ArrayList<>(set);
    }
}
