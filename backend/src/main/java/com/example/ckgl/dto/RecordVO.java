package com.example.ckgl.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 出入库流水列表行 */
@Data
public class RecordVO {

    private Long id;
    private String type;
    private String warehouseName;
    private String locationCode;
    private String partCode;
    private String partName;
    private Integer quantity;
    private String remark;
    private LocalDateTime createdAt;
}
