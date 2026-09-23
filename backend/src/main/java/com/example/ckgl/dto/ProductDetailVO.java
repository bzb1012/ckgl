package com.example.ckgl.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductDetailVO {

    private Long id;
    private String code;
    private String name;
    private String remark;
    private List<ProductPartDetail> parts;
}
