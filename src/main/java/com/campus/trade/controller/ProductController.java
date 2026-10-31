package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.RedisKeys;
import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.dto.ProductRequest;
import com.campus.trade.dto.StatusRequest;
import com.campus.trade.entity.User;
import com.campus.trade.ratelimit.RateLimit;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.ProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 商品：列表搜索、详情、发布、编辑、上下架
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private final UserService userService;

    private final StringRedisTemplate redisTemplate;

    private final com.campus.trade.service.BrowseHistoryService browseHistoryService;

    /** 商品搜索：关键词、分类、价格区间、是否可议价、排序 */
    @GetMapping
    public Result<PageResult<ProductVO>> list(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) BigDecimal minPrice,
                                              @RequestParam(required = false) BigDecimal maxPrice,
                                              @RequestParam(required = false) Integer negotiable,
                                              @RequestParam(defaultValue = "new") String sort,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        if (StringUtils.hasText(keyword)) {
            // 搜索热词统计（Redis ZSet）
            redisTemplate.opsForZSet().incrementScore(RedisKeys.SEARCH_HOT, keyword.trim(), 1);
        }
        return Result.ok(productService.search(keyword, categoryId, minPrice, maxPrice, negotiable, sort,
                page, size));
    }

    /** 热门推荐 */
    @GetMapping("/hot")
    public Result<java.util.List<ProductVO>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(productService.hot(limit));
    }

    /** 我的商品：status 为空表示全部 */
    @GetMapping("/mine")
    @LoginRequired
    public Result<PageResult<ProductVO>> mine(@RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return Result.ok(productService.pageMine(UserContext.requireUserId(), status, page, size));
    }

    /** 商品详情（登录用户可看到收藏状态与联系方式，并记录浏览历史） */
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        ProductVO vo = productService.detail(id, UserContext.getUserId(), UserContext.isAdmin());
        Long userId = UserContext.getUserId();
        if (userId != null) {
            browseHistoryService.record(userId, id);
        }
        return Result.ok(vo);
    }

    /** 发布商品 */
    @PostMapping
    @LoginRequired
    @RateLimit(key = "product-publish", count = 10, seconds = 300, message = "发布过于频繁，请稍后再试")
    public Result<Long> publish(@Valid @RequestBody ProductRequest request) {
        Long userId = UserContext.requireUserId();
        User user = userService.requireUser(userId);
        Long id = productService.publish(userId, request, user.getIdentityStatus());
        return Result.ok("发布成功", id);
    }

    /** 编辑商品 */
    @PutMapping("/{id}")
    @LoginRequired
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        productService.update(UserContext.requireUserId(), id, request);
        return Result.ok("修改成功", null);
    }

    /** 上下架 / 标记已售出：1 上架 2 下架 3 已售出 */
    @PutMapping("/{id}/status")
    @LoginRequired
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        productService.changeStatus(UserContext.requireUserId(), id, request.getStatus());
        return Result.ok("操作成功", null);
    }

    /** 删除自己发布的商品 */
    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteMine(UserContext.requireUserId(), id);
        return Result.ok("已删除", null);
    }

    /** 卖家的在售商品 */
    @GetMapping("/seller/{sellerId}")
    public Result<java.util.List<ProductVO>> sellerProducts(@PathVariable Long sellerId) {
        return Result.ok(productService.listBySeller(sellerId));
    }
}
