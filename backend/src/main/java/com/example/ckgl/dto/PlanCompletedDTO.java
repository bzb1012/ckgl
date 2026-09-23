package com.example.ckgl.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 实时更新计划已完成数量入参 */
@Data
public class PlanCompletedDTO {

    @NotNull(message = "请输入已完成数量")
    @Min(value = 0, message = "已完成数量不能为负数")
    private Integer completed;
}
