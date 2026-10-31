package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.dto.CommentRequest;
import com.campus.trade.security.LoginRequired;
import com.campus.trade.service.CommentService;
import com.campus.trade.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品留言与回复
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 商品留言列表（两级：留言 + 卖家/买家回复） */
    @GetMapping("/products/{productId}/comments")
    public Result<List<CommentVO>> list(@PathVariable Long productId) {
        return Result.ok(commentService.listByProduct(productId));
    }

    /** 发表留言或回复留言（parentId 不为空表示回复） */
    @PostMapping("/products/{productId}/comments")
    @LoginRequired
    public Result<CommentVO> add(@PathVariable Long productId, @Valid @RequestBody CommentRequest request) {
        return Result.ok("留言成功", commentService.add(UserContext.requireUserId(), productId, request));
    }

    /** 删除留言：作者本人或管理员 */
    @DeleteMapping("/comments/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(UserContext.requireUserId(), UserContext.isAdmin(), id);
        return Result.ok("已删除", null);
    }
}
