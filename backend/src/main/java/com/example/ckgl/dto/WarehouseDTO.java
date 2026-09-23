package com.example.ckgl.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseDTO {

    @NotBlank(message = "仓库编码不能为空")
    private String code;
    @NotBlank(message = "仓库名称不能为空")
    private String name;
    private String remark;
}
