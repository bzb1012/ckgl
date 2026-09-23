package com.example.ckgl.dto;

import lombok.Data;

/** 计划关联零件的库存对比行（详情弹窗用） */
@Data
public class PlanPartVO {

    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    /** 单件产品用量 */
    private Integer usage;
    /** 本计划剩余需求 = 剩余数量 × 用量 */
    private Integer need;
    /** 全部未完成计划的该零件总需求 */
    private Integer demand;
    /** 安全库存余量 */
    private Integer safetyStock;
    /** 当前库存(全部货位合计) */
    private Integer stock;
    /** 缺口 = 需求 + 安全库存 - 库存，≤0 视为足够 */
    private Integer lack;
    /** 库存是否足够 */
    private Boolean enough;
}
