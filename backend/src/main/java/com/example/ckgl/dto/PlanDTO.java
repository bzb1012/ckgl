package com.example.ckgl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** 新增/修改生产计划入参 */
@Data
public class PlanDTO {

    @NotNull(message = "请选择计划日期")
    private LocalDate planDate;

    @NotBlank(message = "请选择产线")
    private String line;

    @NotNull(message = "请选择产品")
    private Long productId;

    @NotNull(message = "请输入计划数量")
    @Min(value = 1, message = "计划数量至少为 1")
    private Integer quantity;

    private String remark;
}
