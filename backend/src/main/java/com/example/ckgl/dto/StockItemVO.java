package com.example.ckgl.dto;

import lombok.Data;

/** 盘点物料行：货位内一种零件 */
@Data
public class StockItemVO {

    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    private String category;
    private Integer quantity;
}
