package com.example.ckgl.dto;

import lombok.Data;

import java.time.LocalDate;

/** 计划导入单行数据 */
@Data
public class PlanImportItemDTO {

    private LocalDate planDate;
    private String line;
    private Long productId;
    private Integer quantity;
    private String remark;
}
