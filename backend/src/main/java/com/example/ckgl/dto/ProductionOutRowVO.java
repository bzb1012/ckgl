package com.example.ckgl.dto;

import lombok.Data;

/** 产线出库页行：当日计划需求 + 当日已出库 */
@Data
public class ProductionOutRowVO {

    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    /** 当日计划需求（同型号合并累加） */
    private Integer totalNeed;
    /** 当日涉及的计划数 */
    private Integer planCount;
    /** 当日已出库合计 */
    private Integer outToday;
}
