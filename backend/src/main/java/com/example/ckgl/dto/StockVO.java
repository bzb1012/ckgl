package com.example.ckgl.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 库存列表行：仓库/货位/零件/数量 */
@Data
public class StockVO {

    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    private Integer quantity;
    private LocalDateTime updatedAt;
}
