package com.example.ckgl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OutboundRequest {

    @NotNull(message = "货位不能为空")
    private Long locationId;
    @NotNull(message = "零件不能为空")
    private Long partId;
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;
    private String remark;
}
