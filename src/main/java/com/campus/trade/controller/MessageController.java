package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.MessageService;
import com.campus.trade.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内消息：留言通知、审核结果、举报处理
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@LoginRequired
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<PageResult<MessageVO>> list(@RequestParam(required = false) Integer isRead,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.ok(messageService.page(UserContext.requireUserId(), isRead, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount(UserContext.requireUserId()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(UserContext.requireUserId(), id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead(UserContext.requireUserId());
        return Result.ok("已全部标记为已读", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        messageService.delete(UserContext.requireUserId(), id);
        return Result.ok("已删除", null);
    }
}
