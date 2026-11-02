package com.example.ckgl.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 导入结果：成功覆盖数 + 未匹配到的零件型号 */
@Data
public class OpeningImportVO {

    private int updated;
    private List<String> notFound = new ArrayList<>();
}
