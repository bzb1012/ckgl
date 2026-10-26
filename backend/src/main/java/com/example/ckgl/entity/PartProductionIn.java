package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("part_production_in")
public class PartProductionIn {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long partId;
    /** 生产入库日期 */
    private LocalDate inDate;
    private Integer quantity;
    private String remark;
    private LocalDateTime createdAt;
}
