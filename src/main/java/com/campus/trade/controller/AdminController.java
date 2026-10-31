package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.dto.AdminQuery;
import com.campus.trade.dto.AuditRequest;
import com.campus.trade.dto.CategoryRequest;
import com.campus.trade.dto.ConfigRequest;
import com.campus.trade.dto.HandleReportRequest;
import com.campus.trade.dto.IdentityRequest;
import com.campus.trade.dto.NoticeRequest;
import com.campus.trade.dto.UserStatusRequest;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Notice;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.CategoryService;
import com.campus.trade.service.ConfigService;
import com.campus.trade.service.NoticeService;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.ReportService;
import com.campus.trade.service.StatService;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.ProductVO;
import com.campus.trade.vo.ReportVO;
import com.campus.trade.vo.StatVO;
import com.campus.trade.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理员后台：用户、商品审核、举报、分类、公告、统计、系统配置
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@LoginRequired(admin = true)
public class AdminController {

    private final UserService userService;

    private final ProductService productService;

    private final ReportService reportService;

    private final CategoryService categoryService;

    private final NoticeService noticeService;

    private final StatService statService;

    private final ConfigService configService;

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(AdminQuery query) {
        return Result.ok(userService.pageUsers(query));
    }

    @GetMapping("/users/{id}")
    public Result<UserVO> userDetail(@PathVariable Long id) {
        return Result.ok(userService.toVO(userService.requireUser(id)));
    }

    /** 禁用 / 解封用户：status 0 正常 1 禁用 */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.ok("操作成功", null);
    }

    /** 身份审核：0 待审核 1 通过 2 驳回 */
    @PutMapping("/users/{id}/identity")
    public Result<Void> updateIdentity(@PathVariable Long id, @Valid @RequestBody IdentityRequest request) {
        userService.updateIdentity(id, request.getStatus());
        return Result.ok("审核完成", null);
    }

    // ==================== 商品审核管理 ====================

    @GetMapping("/products")
    public Result<PageResult<ProductVO>> products(AdminQuery query) {
        return Result.ok(productService.pageAdmin(query));
    }

    /** 审核商品：pass=true 通过上架，false 驳回需理由 */
    @PutMapping("/products/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        productService.audit(id, Boolean.TRUE.equals(request.getPass()), request.getReason());
        return Result.ok("审核完成", null);
    }

    /** 违规下架 */
    @PutMapping("/products/{id}/offline")
    public Result<Void> offline(@PathVariable Long id,
                                @RequestParam(required = false) String reason) {
        productService.forceOffline(id, reason);
        return Result.ok("已强制下架", null);
    }

    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.adminDelete(id);
        return Result.ok("已删除", null);
    }

    // ==================== 举报管理 ====================

    @GetMapping("/reports")
    public Result<PageResult<ReportVO>> reports(AdminQuery query) {
        return Result.ok(reportService.pageAdmin(query));
    }

    /** 处理举报：action = offline 下架商品 / warn 警告用户 / ban 封禁用户 / reject 驳回举报 */
    @PutMapping("/reports/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id, @Valid @RequestBody HandleReportRequest request) {
        reportService.handle(UserContext.requireUserId(), id, request);
        return Result.ok("处理完成", null);
    }

    // ==================== 分类管理 ====================

    @GetMapping("/categories")
    public Result<List<Category>> categories() {
        return Result.ok(categoryService.listAll());
    }

    @PostMapping("/categories")
    public Result<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        return Result.ok("新增成功", categoryService.create(request));
    }

    @PutMapping("/categories/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return Result.ok("修改成功", categoryService.update(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok("已删除", null);
    }

    // ==================== 公告轮播管理 ====================

    @GetMapping("/notices")
    public Result<List<Notice>> notices(@RequestParam(required = false) Integer type) {
        return Result.ok(noticeService.listAll(type));
    }

    @PostMapping("/notices")
    public Result<Notice> createNotice(@Valid @RequestBody NoticeRequest request) {
        return Result.ok("新增成功", noticeService.create(request));
    }

    @PutMapping("/notices/{id}")
    public Result<Notice> updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return Result.ok("修改成功", noticeService.update(id, request));
    }

    @DeleteMapping("/notices/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok("已删除", null);
    }

    // ==================== 数据统计 ====================

    @GetMapping("/stats")
    public Result<StatVO> stats() {
        return Result.ok(statService.overview());
    }

    /** 手动刷新统计缓存 */
    @PutMapping("/stats/refresh")
    public Result<StatVO> refreshStats() {
        statService.evict();
        return Result.ok("统计已刷新", statService.overview());
    }

    // ==================== 系统配置 ====================

    @GetMapping("/configs")
    public Result<Map<String, String>> configs() {
        return Result.ok(configService.all());
    }

    @PutMapping("/configs")
    public Result<Void> saveConfig(@Valid @RequestBody ConfigRequest request) {
        configService.save(request.getConfigKey(), request.getConfigValue(), "后台修改");
        return Result.ok("配置已保存", null);
    }
}
