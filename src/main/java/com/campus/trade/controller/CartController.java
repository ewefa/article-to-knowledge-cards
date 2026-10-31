package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.CartService;
import com.campus.trade.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 意向清单（购物车）接口，数据仅存 Redis，7 天过期
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@LoginRequired
public class CartController {

    private final CartService cartService;

    /** 我的意向清单 */
    @GetMapping
    public Result<Map<String, Object>> list() {
        Long userId = UserContext.requireUserId();
        List<ProductVO> items = cartService.list(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("count", items.size());
        return Result.ok(data);
    }

    /** 数量（用于顶部角标） */
    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.ok(cartService.count(UserContext.requireUserId()));
    }

    /** 加入 */
    @PostMapping("/{productId}")
    public Result<Map<String, Object>> add(@PathVariable Long productId) {
        int count = cartService.add(UserContext.requireUserId(), productId);
        return Result.ok("已加入意向清单", Map.of("count", count));
    }

    /** 移出 */
    @DeleteMapping("/{productId}")
    public Result<Map<String, Object>> remove(@PathVariable Long productId) {
        int count = cartService.remove(UserContext.requireUserId(), productId);
        return Result.ok("已移出意向清单", Map.of("count", count));
    }

    /** 清空 */
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(UserContext.requireUserId());
        return Result.ok("已清空意向清单", null);
    }
}
