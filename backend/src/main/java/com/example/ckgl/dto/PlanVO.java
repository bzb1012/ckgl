package com.example.ckgl.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 生产计划列表行 */
@Data
public class PlanVO {

    private Long id;
    private LocalDate planDate;
    private String line;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private Integer completed;
    private String remark;
    /** 是否缺料（有零件库存无法满足未完成计划总需求+安全库存） */
    private Boolean shortage;
    /** 全部关联零件的库存对比明细 */
    private List<PlanPartVO> parts;
    /** 缺料零件明细（parts 中库存不足的子集） */
    private List<PlanPartVO> shortages;
}
