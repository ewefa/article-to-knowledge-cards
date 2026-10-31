package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.common.RedisCache;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.dto.CategoryRequest;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务：前台展示（Redis 缓存）+ 后台维护
 */
@Service
@RequiredArgsConstructor
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    private final ProductMapper productMapper;

    private final RedisCache redisCache;

    private final StringRedisTemplate redisTemplate;

    /** 前台分类（启用状态），缓存 30 分钟 */
    public List<Category> listEnabled() {
        List<Category> cached = redisCache.getList(RedisKeys.HOME_CATEGORIES, Category.class);
        if (cached != null) {
            return cached;
        }
        List<Category> list = list(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
        redisCache.set(RedisKeys.HOME_CATEGORIES, list, Duration.ofMinutes(30));
        return list;
    }

    public List<Category> listAll() {
        return list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
    }

    public Category create(CategoryRequest request) {
        Category category = new Category();
        apply(category, request);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        save(category);
        evict();
        return category;
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = getById(id);
        if (category == null) {
            throw BizException.notFound("分类不存在");
        }
        apply(category, request);
        category.setUpdateTime(LocalDateTime.now());
        updateById(category);
        evict();
        return category;
    }

    public void delete(Long id) {
        long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id)
                .ne(Product::getStatus, 2));
        if (count > 0) {
            throw new BizException("该分类下还有商品，无法删除");
        }
        removeById(id);
        evict();
    }

    public void evict() {
        redisTemplate.delete(RedisKeys.HOME_CATEGORIES);
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.getName().trim());
        category.setIcon(request.getIcon());
        category.setSort(request.getSort() == null ? 99 : request.getSort());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }
}
