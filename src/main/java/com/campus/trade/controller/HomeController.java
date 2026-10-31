package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Notice;
import com.campus.trade.service.HomeService;
import com.campus.trade.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 首页数据：轮播、分类、热门推荐、最新发布
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /** 首页首屏聚合数据（一次请求拿全） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(homeService.overview());
    }

    @GetMapping("/banners")
    public Result<List<Notice>> banners() {
        return Result.ok(homeService.banners());
    }

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(homeService.categories());
    }

    @GetMapping("/hot")
    public Result<List<ProductVO>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(homeService.hot(limit));
    }

    @GetMapping("/latest")
    public Result<List<ProductVO>> latest(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(homeService.latest(limit));
    }

    @GetMapping("/notices")
    public Result<List<Notice>> notices() {
        return Result.ok(homeService.notices());
    }

    /** 搜索热词（Redis ZSet 统计） */
    @GetMapping("/keywords")
    public Result<List<String>> keywords(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(homeService.hotKeywords(limit));
    }
}
