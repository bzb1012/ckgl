package com.example.ckgl.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationDTO {

    @NotBlank(message = "货位编号不能为空")
    private String code;
    private String remark;
}
