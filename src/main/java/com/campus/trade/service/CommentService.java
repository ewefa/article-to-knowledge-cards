package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.dto.CommentRequest;
import com.campus.trade.entity.Comment;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.CommentMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品留言：提问、卖家回复、留言通知
 */
@Service
@RequiredArgsConstructor
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    private final ProductMapper productMapper;

    private final UserMapper userMapper;

    private final MessageService messageService;

    private final StringRedisTemplate redisTemplate;

    /** 留言列表：顶级留言 + 各自的回复 */
    public List<CommentVO> listByProduct(Long productId) {
        List<Comment> comments = list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getProductId, productId)
                .orderByAsc(Comment::getCreateTime));
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<>();
        for (Comment comment : comments) {
            userIds.add(comment.getUserId());
            if (comment.getReplyUserId() != null) {
                userIds.add(comment.getReplyUserId());
            }
        }
        Map<Long, User> users = new HashMap<>();
        for (User user : userMapper.selectBatchIds(userIds)) {
            users.put(user.getId(), user);
        }
        Map<Long, CommentVO> topLevel = new java.util.LinkedHashMap<>();
        List<CommentVO> replies = new ArrayList<>();
        for (Comment comment : comments) {
            CommentVO vo = toVO(comment, users);
            if (comment.getParentId() == null || comment.getParentId() == 0) {
                topLevel.put(comment.getId(), vo);
            } else {
                replies.add(vo);
            }
        }
        for (CommentVO reply : replies) {
            CommentVO parent = topLevel.get(reply.getParentId());
            if (parent != null) {
                if (parent.getReplies() == null) {
                    parent.setReplies(new ArrayList<>());
                }
                parent.getReplies().add(reply);
            } else {
                // 父留言已被删除时，作为顶级留言展示
                topLevel.put(reply.getId(), reply);
            }
        }
        return new ArrayList<>(topLevel.values());
    }

    /** 发表留言或回复，并给对方发站内消息 */
    @Transactional(rollbackFor = Exception.class)
    public CommentVO add(Long userId, Long productId, CommentRequest request) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw BizException.notFound("商品不存在");
        }
        User author = userMapper.selectById(userId);
        Comment comment = new Comment();
        comment.setProductId(productId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        comment.setParentId(0L);
        comment.setCreateTime(LocalDateTime.now());

        Comment parent = null;
        if (request.getParentId() != null && request.getParentId() > 0) {
            parent = getById(request.getParentId());
            if (parent == null || !parent.getProductId().equals(productId)) {
                throw new BizException("回复的留言不存在");
            }
            comment.setParentId(parent.getId());
            comment.setReplyUserId(parent.getUserId());
        }
        save(comment);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("comment_count = comment_count + 1"));

        String authorName = displayName(author);
        if (parent != null && !parent.getUserId().equals(userId)) {
            messageService.send(parent.getUserId(), "REPLY", "你的留言收到回复",
                    authorName + " 回复：" + comment.getContent(), productId);
        } else if (!product.getUserId().equals(userId)) {
            messageService.send(product.getUserId(), "COMMENT", "你的商品收到新留言",
                    authorName + " 留言：" + comment.getContent(), productId);
        }
        redisTemplate.delete("home:hot:list");
        Map<Long, User> users = new HashMap<>();
        if (author != null) {
            users.put(author.getId(), author);
        }
        if (comment.getReplyUserId() != null) {
            User replyUser = userMapper.selectById(comment.getReplyUserId());
            if (replyUser != null) {
                users.put(replyUser.getId(), replyUser);
            }
        }
        return toVO(comment, users);
    }

    /** 删除留言（本人或管理员） */
    public void delete(Long userId, boolean admin, Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null) {
            throw BizException.notFound("留言不存在");
        }
        if (!admin && !comment.getUserId().equals(userId)) {
            throw BizException.forbidden("只能删除自己的留言");
        }
        removeById(commentId);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, comment.getProductId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
    }

    private CommentVO toVO(Comment comment, Map<Long, User> users) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setProductId(comment.getProductId());
        vo.setUserId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setReplyUserId(comment.getReplyUserId());
        vo.setCreateTime(comment.getCreateTime());
        User author = users.get(comment.getUserId());
        if (author != null) {
            vo.setNickname(displayName(author));
            vo.setAvatar(author.getAvatar());
        }
        if (comment.getReplyUserId() != null) {
            User replyUser = users.get(comment.getReplyUserId());
            if (replyUser != null) {
                vo.setReplyNickname(displayName(replyUser));
            }
        }
        return vo;
    }

    private String displayName(User user) {
        if (user == null) {
            return "匿名用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getName();
    }
}
