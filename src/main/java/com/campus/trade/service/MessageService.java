package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.trade.common.BizException;
import com.campus.trade.common.PageResult;
import com.campus.trade.entity.Message;
import com.campus.trade.mapper.MessageMapper;
import com.campus.trade.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 站内消息：留言通知、审核 / 举报处理结果
 */
@Service
@RequiredArgsConstructor
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    public Message send(Long userId, String type, String title, String content, Long relatedId) {
        Message message = new Message();
        message.setUserId(userId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedId(relatedId);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        save(message);
        return message;
    }

    public PageResult<MessageVO> page(Long userId, Integer isRead, int page, int size) {
        Page<Message> result = page(Page.of(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(isRead != null, Message::getIsRead, isRead)
                        .orderByDesc(Message::getCreateTime));
        List<MessageVO> records = new ArrayList<>();
        for (Message message : result.getRecords()) {
            records.add(toVO(message));
        }
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public long unreadCount(Long userId) {
        return count(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    public void markRead(Long userId, Long messageId) {
        Message message = getById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw BizException.notFound("消息不存在");
        }
        message.setIsRead(1);
        updateById(message);
    }

    public void markAllRead(Long userId) {
        update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }

    public void delete(Long userId, Long messageId) {
        Message message = getById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw BizException.notFound("消息不存在");
        }
        removeById(messageId);
    }

    public MessageVO toVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setType(message.getType());
        vo.setTypeText(typeText(message.getType()));
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setRelatedId(message.getRelatedId());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    public static String typeText(String type) {
        if (type == null) {
            return "通知";
        }
        return switch (type) {
            case "COMMENT" -> "商品留言";
            case "REPLY" -> "留言回复";
            case "AUDIT" -> "审核结果";
            case "REPORT" -> "举报处理";
            case "SYSTEM" -> "系统通知";
            default -> "通知";
        };
    }
}
