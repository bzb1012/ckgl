package com.example.ckgl.dto;

import lombok.Data;

/** 产品详情中的零件明细行 */
@Data
public class ProductPartDetail {

    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    private String category;
    private Integer quantity;
}
