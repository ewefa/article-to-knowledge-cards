package com.campus.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;

    private Long productId;

    private Long userId;

    private String nickname;

    private String avatar;

    private String content;

    private Long parentId;

    private Long replyUserId;

    private String replyNickname;

    private LocalDateTime createTime;

    private List<CommentVO> replies;
}
