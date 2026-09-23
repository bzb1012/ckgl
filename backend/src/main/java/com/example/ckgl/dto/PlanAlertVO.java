package com.example.ckgl.dto;

import lombok.Data;

/** 库存预警汇总行（按零件维度聚合） */
@Data
public class PlanAlertVO {

    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    /** 全部未完成计划的该零件总需求 */
    private Integer demand;
    /** 当前库存(全部货位合计) */
    private Integer stock;
    /** 安全库存余量：库存需大于需求该数量 */
    private Integer safetyStock;
    /** 缺口 = 需求 + 安全库存 - 库存 */
    private Integer lack;
    /** 涉及的未完成计划数 */
    private Integer planCount;
}
