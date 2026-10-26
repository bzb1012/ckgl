package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 生产台账月度结算：某月的期初库存（由上月期末结转） */
@Data
@TableName("part_month_settle")
public class PartMonthSettle {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long partId;
    /** 结算后的月份 YYYY-MM，该行的 openingQty 即该月期初 */
    private String monthKey;
    private Integer openingQty;
    private LocalDateTime settledAt;
}
