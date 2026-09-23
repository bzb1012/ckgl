package com.example.ckgl.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PartDTO {

    @NotBlank(message = "零件型号不能为空")
    private String code;
    @NotBlank(message = "零件名称不能为空")
    private String name;
    private String unit;
    private String category;
    private String remark;
}
