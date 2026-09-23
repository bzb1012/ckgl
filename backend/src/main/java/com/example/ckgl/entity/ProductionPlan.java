package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("production_plan")
public class ProductionPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate planDate;
    /** 产线(1线/2线/3线...) */
    private String line;
    private Long productId;
    /** 计划数量 */
    private Integer quantity;
    /** 已完成数量(实时更新，用于扣除已生产部分，避免预警误报) */
    private Integer completed;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
