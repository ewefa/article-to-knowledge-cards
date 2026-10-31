package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.entity.Favorite;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * 收藏业务：收藏 / 取消收藏、我的收藏列表，同步维护商品收藏数
 */
@Service
@RequiredArgsConstructor
public class FavoriteService extends ServiceImpl<FavoriteMapper, Favorite> {

    private final ProductMapper productMapper;

    private final ProductService productService;

    private final StringRedisTemplate redisTemplate;

    /** 切换收藏状态，返回收藏后的状态 */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        Favorite exists = getOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId), false);
        boolean favored;
        if (exists == null) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setProductId(productId);
            favorite.setCreateTime(LocalDateTime.now());
            save(favorite);
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, productId)
                    .setSql("favorite_count = favorite_count + 1"));
            favored = true;
        } else {
            removeById(exists.getId());
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, productId)
                    .setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
            favored = false;
        }
        redisTemplate.delete(RedisKeys.favoriteIds(userId));
        redisTemplate.delete(RedisKeys.productDetail(productId));
        return favored;
    }

    public boolean isFavorited(Long userId, Long productId) {
        return favoriteIds(userId).contains(productId);
    }

    /** 用户收藏的商品 ID，优先读 Redis 缓存（30 分钟），收藏/取消时清除 */
    public List<Long> favoriteIds(Long userId) {
        String cached = redisTemplate.opsForValue().get(RedisKeys.favoriteIds(userId));
        if (cached != null) {
            if (cached.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> ids = new ArrayList<>();
            for (String part : cached.split(",")) {
                try {
                    ids.add(Long.valueOf(part));
                } catch (NumberFormatException ignored) {
                    // 忽略脏数据
                }
            }
            return ids;
        }
        List<Favorite> favorites = list(new LambdaQueryWrapper<Favorite>()
                .select(Favorite::getProductId)
                .eq(Favorite::getUserId, userId));
        List<Long> ids = new ArrayList<>(favorites.size());
        for (Favorite favorite : favorites) {
            ids.add(favorite.getProductId());
        }
        StringJoiner joiner = new StringJoiner(",");
        ids.forEach(id -> joiner.add(String.valueOf(id)));
        redisTemplate.opsForValue().set(RedisKeys.favoriteIds(userId), joiner.toString(), Duration.ofMinutes(30));
        return ids;
    }

    /** 我的收藏（按收藏时间倒序） */
    public PageResult<ProductVO> myFavorites(Long userId, int page, int size) {
        Page<Favorite> result = page(Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 60)),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));
        List<Favorite> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<Long> productIds = new ArrayList<>();
        for (Favorite favorite : records) {
            productIds.add(favorite.getProductId());
        }
        List<Product> products = productMapper.selectBatchIds(productIds);
        products.sort((a, b) -> productIds.indexOf(a.getId()) - productIds.indexOf(b.getId()));
        return PageResult.of(productService.toVOList(products, userId), result.getTotal(),
                result.getCurrent(), result.getSize());
    }
}
