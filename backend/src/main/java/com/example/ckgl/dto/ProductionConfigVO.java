package com.example.ckgl.dto;

import lombok.Data;

/** 生产出入库配置（读取视图，含默认值） */
@Data
public class ProductionConfigVO {

    private Integer hourlyQty;
    private Integer lineCount;
    private Integer alertThreshold;
    private String barkUrl;
}
