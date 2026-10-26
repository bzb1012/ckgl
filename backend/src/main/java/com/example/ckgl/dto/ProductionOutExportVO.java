package com.example.ckgl.dto;

import lombok.Data;

import java.util.List;

/** 产线出库导出数据：一个日期区间的工作簿，含合计 + 每日明细 */
@Data
public class ProductionOutExportVO {

    /** 区间合计：逐日合并累加 */
    private List<DailyRow> total;

    /** 每日明细，每个日期一个工作表 */
    private List<DaySheet> days;

    @Data
    public static class DailyRow {
        private String partCode;
        private String partName;
        private String unit;
        private Integer totalNeed;
        private Integer planCount;
        private Integer outTotal;
    }

    @Data
    public static class DaySheet {
        private String date;
        private List<DailyRow> rows;
    }
}