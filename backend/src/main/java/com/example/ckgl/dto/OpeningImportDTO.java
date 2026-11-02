package com.example.ckgl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 导入覆盖上月库存：按零件型号匹配，数量写入所选月份期初 */
@Data
public class OpeningImportDTO {

    @NotBlank(message = "月份不能为空")
    private String month;

    @NotEmpty(message = "导入数据不能为空")
    private List<Row> items;

    @Data
    public static class Row {
        @NotBlank(message = "零件型号不能为空")
        private String partCode;
        private Integer quantity;
    }
}
