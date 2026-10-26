package com.example.ckgl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** 每日入库单元格修改参数：直接设置该零件某天的入库总量 */
@Data
public class ProductionDayInDTO {

    @NotNull(message = "请选择零件")
    private Long partId;

    @NotNull(message = "请选择日期")
    private LocalDate date;

    @NotNull(message = "请输入数量")
    @Min(value = 0, message = "入库数量不能小于 0")
    private Integer quantity;
}
