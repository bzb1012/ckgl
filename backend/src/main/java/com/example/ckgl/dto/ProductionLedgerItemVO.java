package com.example.ckgl.dto;

import lombok.Data;

import java.util.List;

/** 生产台账行：上月库存 + 本月入库 - 本月消耗 = 本月库存 */
@Data
public class ProductionLedgerItemVO {

    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    private String category;
    /** 上月库存（上月期末结转） */
    private Integer opening;
    /** 本月生产入库合计 */
    private Integer inTotal;
    /** 本月消耗合计 */
    private Integer outTotal;
    /** 本月库存 = 上月库存 + 本月入库 - 本月消耗 */
    private Integer current;
    /** 每日消耗，下标 0 对应 1 号 */
    private List<Integer> dailyOut;
    /** 每日生产入库，下标 0 对应 1 号 */
    private List<Integer> dailyIn;
}
