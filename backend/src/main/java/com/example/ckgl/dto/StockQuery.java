package com.example.ckgl.dto;

import lombok.Data;

@Data
public class StockQuery {

    private Long warehouseId;
    private Long locationId;
    private Long partId;
    /** 零件型号/名称/货位编号 模糊 */
    private String keyword;
    private Integer page = 1;
    private Integer size = 10;
}
