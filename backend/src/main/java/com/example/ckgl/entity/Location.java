package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("location")
public class Location {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warehouseId;
    private String code;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
