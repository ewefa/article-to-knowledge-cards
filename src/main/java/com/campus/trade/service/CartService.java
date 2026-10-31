package com.campus.trade.service;

import com.campus.trade.common.BizException;
import com.campus.trade.common.ProductStatus;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.entity.Product;
import com.campus.trade.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 意向清单（购物车）：纯 Redis 临时数据，不落 MySQL。
 * 平台不做线上支付，这里只是把想当面交易的闲置先记下来，方便和卖家约时间。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    /** 临时数据保留 7 天 */
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ProductService productService;

    /** 加入意向清单，返回当前清单数量 */
    public int add(Long userId, Long productId) {
        Product product = productService.getById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BizException("该商品已下架或已售出，无法加入意向清单");
        }
        if (product.getUserId().equals(userId)) {
            throw new BizException("这是你自己发布的商品");
        }
        String key = RedisKeys.cart(userId);
        redisTemplate.opsForHash().put(key, String.valueOf(productId), String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(key, TTL);
        return count(userId);
    }

    /** 移出意向清单 */
    public int remove(Long userId, Long productId) {
        redisTemplate.opsForHash().delete(RedisKeys.cart(userId), String.valueOf(productId));
        return count(userId);
    }

    /** 清空意向清单 */
    public void clear(Long userId) {
        redisTemplate.delete(RedisKeys.cart(userId));
    }

    /** 当前清单数量 */
    public int count(Long userId) {
        Long size = redisTemplate.opsForHash().size(RedisKeys.cart(userId));
        return size == null ? 0 : size.intValue();
    }

    /** 清单详情：按加入时间倒序返回商品信息 */
    public List<ProductVO> list(Long userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.cart(userId));
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                ids.add(Long.valueOf(String.valueOf(entry.getKey())));
            } catch (NumberFormatException ignored) {
                // 脏数据直接忽略
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Product> products = productService.listByIds(ids);
        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        // 让出参顺序与加入时间一致：后加入的排前面
        products.sort((a, b) -> Long.compare(
                Long.parseLong(String.valueOf(entries.getOrDefault(String.valueOf(b.getId()), "0"))),
                Long.parseLong(String.valueOf(entries.getOrDefault(String.valueOf(a.getId()), "0")))));
        return productService.toVOList(products, userId);
    }

    /** 清理已失效（被删除或下架）的清单项 */
    public int cleanup(Long userId) {
        String key = RedisKeys.cart(userId);
        Set<Object> fields = redisTemplate.opsForHash().keys(key);
        if (fields == null || fields.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (Object field : fields) {
            Product product = productService.getById(Long.valueOf(String.valueOf(field)));
            if (product == null || product.getStatus() != ProductStatus.ON_SALE) {
                redisTemplate.opsForHash().delete(key, field);
                removed++;
            }
        }
        return removed;
    }
}
