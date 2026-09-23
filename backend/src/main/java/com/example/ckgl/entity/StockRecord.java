package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_record")
public class StockRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** IN=入库 OUT=出库 */
    private String type;
    /** 冗余仓库id，便于按仓库筛选 */
    private Long warehouseId;
    private Long locationId;
    private Long partId;
    /** 本次出入库数量（正数） */
    private Integer quantity;
    private String remark;
    private LocalDateTime createdAt;
}
