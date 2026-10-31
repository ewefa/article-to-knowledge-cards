package com.campus.trade.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 统计图表数据项 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartItem {

    private String name;

    private Long value;
}
