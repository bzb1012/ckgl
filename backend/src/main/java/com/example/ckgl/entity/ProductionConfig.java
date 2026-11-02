package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 生产出入库配置（键值对）：每小时出库量/生产线数/预警阈值/Bark 推送地址 */
@Data
@TableName("production_config")
public class ProductionConfig {

    @TableId
    private String configKey;
    private String configValue;
}
