package com.campus.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 300, message = "留言内容最多 300 个字符")
    private String content;

    /** 父留言 ID，回复某条留言时传入 */
    private Long parentId;

    /** 被回复用户 ID（回复时由后端校验） */
    private Long replyUserId;
}
