package com.example.ckgl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 月度结算参数：把上月期末结转为本月期初 */
@Data
public class ProductionSettleDTO {

    @NotBlank(message = "请选择月份")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式应为 YYYY-MM")
    private String month;
}
