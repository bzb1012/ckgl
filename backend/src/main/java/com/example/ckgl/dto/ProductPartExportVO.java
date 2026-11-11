package com.example.ckgl.dto;

import lombok.Data;

/** 产品零件导出明细行（产品型号 + 所需零件） */
@Data
public class ProductPartExportVO {

    /** 仅用于分组，不参与导出 */
    private Long productId;
    private String productCode;
    private String productName;
    private String partCode;
    private String partName;
    /** 零件类别（导出表头） */
    private String category;
    private String unit;
    private Integer quantity;
}
