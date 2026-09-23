package com.example.ckgl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductPartItem {

    @NotNull(message = "零件不能为空")
    private Long partId;
    @NotNull(message = "零件用量不能为空")
    @Min(value = 1, message = "零件用量必须大于0")
    private Integer quantity;
}
