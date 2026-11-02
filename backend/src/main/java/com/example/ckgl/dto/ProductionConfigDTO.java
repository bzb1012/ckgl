package com.example.ckgl.dto;

import lombok.Data;

/** 生产出入库配置：定时出库与库存预警 */
@Data
public class ProductionConfigDTO {

    /** 每条线每小时每个零件的出库数量 */
    private Integer hourlyQty;

    /** 生产线数量 */
    private Integer lineCount;

    /** 本月库存低于该值时预警 */
    private Integer alertThreshold;

    /** Bark 推送地址前缀，如 https://api.day.app/你的key，空则不推送 */
    private String barkUrl;
}
