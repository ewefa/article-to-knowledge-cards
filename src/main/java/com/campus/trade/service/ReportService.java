package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.dto.AdminQuery;
import com.campus.trade.dto.HandleReportRequest;
import com.campus.trade.dto.ReportRequest;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.Report;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.ReportMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 举报业务：用户举报商品，管理员核实处理（下架 / 警告 / 封禁 / 驳回）
 */
@Service
@RequiredArgsConstructor
public class ReportService extends ServiceImpl<ReportMapper, Report> {

    public static final int PENDING = 0;

    public static final int HANDLED = 1;

    public static final int REJECTED = 2;

    private final ProductMapper productMapper;

    private final UserMapper userMapper;

    private final UserService userService;

    private final ProductService productService;

    private final MessageService messageService;

    /** 提交举报：同一用户对同一商品不能重复举报（待处理状态下） */
    public void submit(Long userId, Long productId, ReportRequest request) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        if (product.getUserId().equals(userId)) {
            throw new BizException("不能举报自己发布的商品");
        }
        long exists = count(new LambdaQueryWrapper<Report>()
                .eq(Report::getProductId, productId)
                .eq(Report::getUserId, userId)
                .eq(Report::getStatus, PENDING));
        if (exists > 0) {
            throw new BizException("你已举报过该商品，请等待管理员处理");
        }
        Report report = new Report();
        report.setProductId(productId);
        report.setUserId(userId);
        report.setReason(request.getReason().trim());
        report.setDescription(request.getDescription());
        report.setStatus(PENDING);
        report.setCreateTime(LocalDateTime.now());
        save(report);
    }

    /** 管理员：举报列表 */
    public PageResult<ReportVO> pageAdmin(AdminQuery query) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(query.getStatus() != null, Report::getStatus, query.getStatus())
                .orderByDesc(Report::getCreateTime);
        Page<Report> result = page(Page.of(Math.max(query.getPage(), 1),
                Math.min(Math.max(query.getSize(), 1), 100)), wrapper);
        List<Report> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(java.util.Collections.emptyList(), result.getTotal(),
                    result.getCurrent(), result.getSize());
        }
        Set<Long> productIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (Report report : records) {
            productIds.add(report.getProductId());
            userIds.add(report.getUserId());
        }
        Map<Long, Product> products = new HashMap<>();
        for (Product product : productMapper.selectBatchIds(productIds)) {
            products.put(product.getId(), product);
            userIds.add(product.getUserId());
        }
        Map<Long, User> users = new HashMap<>();
        for (User user : userMapper.selectBatchIds(userIds)) {
            users.put(user.getId(), user);
        }
        List<ReportVO> list = new ArrayList<>();
        for (Report report : records) {
            ReportVO vo = new ReportVO();
            vo.setId(report.getId());
            vo.setProductId(report.getProductId());
            vo.setUserId(report.getUserId());
            vo.setReason(report.getReason());
            vo.setDescription(report.getDescription());
            vo.setStatus(report.getStatus());
            vo.setStatusText(statusText(report.getStatus()));
            vo.setHandleRemark(report.getHandleRemark());
            vo.setHandleTime(report.getHandleTime());
            vo.setCreateTime(report.getCreateTime());
            Product product = products.get(report.getProductId());
            if (product != null) {
                vo.setProductTitle(product.getTitle());
                vo.setProductCover(product.getCover());
                vo.setSellerId(product.getUserId());
            }
            User reporter = users.get(report.getUserId());
            if (reporter != null) {
                vo.setReporterName(displayName(reporter));
            }
            if (vo.getSellerId() != null) {
                User seller = users.get(vo.getSellerId());
                if (seller != null) {
                    vo.setSellerName(displayName(seller));
                }
            }
            list.add(vo);
        }
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 管理员：处理举报 */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long adminId, Long reportId, HandleReportRequest request) {
        Report report = getById(reportId);
        if (report == null) {
            throw BizException.notFound("举报记录不存在");
        }
        if (report.getStatus() != PENDING) {
            throw new BizException("该举报已处理");
        }
        Product product = productMapper.selectById(report.getProductId());
        Long sellerId = product == null ? null : product.getUserId();
        String remark = StringUtils.hasText(request.getRemark()) ? request.getRemark() : null;

        switch (request.getAction()) {
            case "offline" -> {
                productService.forceOffline(report.getProductId(), remark);
                notifySeller(sellerId, "商品已被下架",
                        "你的商品《" + (product == null ? "" : product.getTitle()) + "》经核实存在违规，已强制下架。"
                                + (remark == null ? "" : "处理说明：" + remark), report.getProductId());
            }
            case "warn" -> notifySeller(sellerId, "收到平台警告",
                    "你的商品《" + (product == null ? "" : product.getTitle()) + "》被举报，请自觉遵守平台规范。"
                            + (remark == null ? "" : "说明：" + remark), report.getProductId());
            case "ban" -> {
                if (sellerId != null) {
                    userService.updateStatus(sellerId, 1);
                }
                notifySeller(sellerId, "账号已被封禁",
                        "经举报核实，你的账号存在严重违规行为，已被封禁。" + (remark == null ? "" : "说明：" + remark),
                        report.getProductId());
            }
            case "reject" -> {
                report.setStatus(REJECTED);
                report.setHandleRemark(remark);
                report.setHandlerId(adminId);
                report.setHandleTime(LocalDateTime.now());
                updateById(report);
                messageService.send(report.getUserId(), "REPORT", "举报已处理",
                        "你举报的商品经核实未发现违规，感谢你的关注。", report.getProductId());
                return;
            }
            default -> throw new BizException("不支持的处理方式");
        }
        report.setStatus(HANDLED);
        report.setHandleRemark(remark);
        report.setHandlerId(adminId);
        report.setHandleTime(LocalDateTime.now());
        updateById(report);
        messageService.send(report.getUserId(), "REPORT", "举报已处理",
                "你举报的商品已处理，处理方式：" + actionText(request.getAction()),
                report.getProductId());
    }

    private void notifySeller(Long sellerId, String title, String content, Long productId) {
        if (sellerId != null) {
            messageService.send(sellerId, "REPORT", title, content, productId);
        }
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case PENDING -> "待处理";
            case HANDLED -> "已处理";
            case REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    private String actionText(String action) {
        return switch (action) {
            case "offline" -> "下架商品";
            case "warn" -> "警告用户";
            case "ban" -> "封禁用户";
            case "reject" -> "驳回举报";
            default -> action;
        };
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getName();
    }
}
