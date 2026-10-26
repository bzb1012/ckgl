package com.example.ckgl.dto;

import lombok.Data;

import java.util.List;

/** 生产台账某月的表格数据 */
@Data
public class ProductionLedgerVO {

    /** 月份 YYYY-MM */
    private String month;
    /** 该月天数，前端据此渲染每日列 */
    private int days;
    private List<ProductionLedgerItemVO> items;
}
