package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("warehouse")
public class Warehouse {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 列表展示用：货位数，非表字段 */
    @TableField(exist = false)
    private Integer locationCount;
}
