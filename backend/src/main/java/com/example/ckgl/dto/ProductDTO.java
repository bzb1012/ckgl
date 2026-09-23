package com.example.ckgl.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {

    @NotBlank(message = "产品编码不能为空")
    private String code;
    @NotBlank(message = "产品名称不能为空")
    private String name;
    private String remark;
    /** 绑定的零件及用量，全量提交，允许为空 */
    @Valid
    private List<ProductPartItem> parts;
}
