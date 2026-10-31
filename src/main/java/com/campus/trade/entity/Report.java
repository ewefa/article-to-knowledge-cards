package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;

/**
 * 商品举报实体
 */
@Data
@TableName("t_report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long userId;

    private String reason;

    private String description;

    private Integer status;

    private String handleRemark;

    private Long handlerId;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
