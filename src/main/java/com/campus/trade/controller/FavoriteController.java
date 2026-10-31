package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 收藏 / 取消收藏
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 切换收藏状态，返回 {favorited: true/false} */
    @PostMapping("/{productId}/toggle")
    @LoginRequired
    public Result<Map<String, Object>> toggle(@PathVariable Long productId) {
        boolean favorited = favoriteService.toggle(UserContext.requireUserId(), productId);
        return Result.ok(favorited ? "收藏成功" : "已取消收藏", Map.of("favorited", favorited));
    }

    /** 查询是否已收藏 */
    @GetMapping("/{productId}/check")
    public Result<Map<String, Object>> check(@PathVariable Long productId) {
        Long userId = UserContext.getUserId();
        boolean favorited = userId != null && favoriteService.isFavorited(userId, productId);
        return Result.ok(Map.of("favorited", favorited));
    }
}
