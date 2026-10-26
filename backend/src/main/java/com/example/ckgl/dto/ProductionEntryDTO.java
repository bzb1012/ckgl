package com.example.ckgl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** 生产台账出入库录入参数（入库/出库共用） */
@Data
public class ProductionEntryDTO {

    @NotNull(message = "请选择零件")
    private Long partId;

    @NotNull(message = "请选择日期")
    private LocalDate date;

    @NotNull(message = "请输入数量")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    private String remark;
}
