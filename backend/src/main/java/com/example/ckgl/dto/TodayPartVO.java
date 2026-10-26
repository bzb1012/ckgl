package com.example.ckgl.dto;

import lombok.Data;

/** 每日用料汇总行：所选日期全部计划所需零件，同型号合并累加（不考虑已完成数量） */
@Data
public class TodayPartVO {

    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    /** 需求总数：Σ(各计划计划数量 × 单件用量) */
    private Integer totalNeed;
    /** 其中已完成计划的需求：Σ(已完成或过去日期计划 × 单件用量) */
    private Integer completedNeed;
    /** 该零件被几个今日计划用到 */
    private Integer planCount;
    /** 当前库存(全部货位合计) */
    private Integer stock;
}
