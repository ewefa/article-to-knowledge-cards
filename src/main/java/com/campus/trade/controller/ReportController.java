package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.dto.ReportRequest;
import com.campus.trade.ratelimit.RateLimit;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 举报商品
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/products/{productId}/reports")
    @LoginRequired
    @RateLimit(key = "report", count = 10, seconds = 600, message = "举报过于频繁，请稍后再试")
    public Result<Void> submit(@PathVariable Long productId, @Valid @RequestBody ReportRequest request) {
        reportService.submit(UserContext.requireUserId(), productId, request);
        return Result.ok("举报已提交，管理员会尽快核实处理", null);
    }
}
