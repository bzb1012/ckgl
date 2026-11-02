package com.example.ckgl.dto;

import lombok.Data;

/** 生产出入库配置：定时出库与库存预警 */
@Data
public class ProductionConfigDTO {

    /** 每条产线每个周期完成的计划产品数量 */
    private Integer intervalQty;

    /** 执行间隔（分钟），每天 8:00~20:00 生效 */
    private Integer intervalMin;

    /** 本月库存低于该值时预警 */
    private Integer alertThreshold;

    /** Bark 推送地址前缀，如 https://api.day.app/你的key，空则不推送 */
    private String barkUrl;
}
