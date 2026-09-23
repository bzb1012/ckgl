package com.example.ckgl.dto;

import lombok.Data;

@Data
public class RecordQuery {

    /** IN / OUT，可空 */
    private String type;
    private Long warehouseId;
    private Long locationId;
    private Long partId;
    /** yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss */
    private String startTime;
    private String endTime;
    private Integer page = 1;
    private Integer size = 10;
}
