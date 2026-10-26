package com.example.ckgl.controller;

import com.example.ckgl.common.Result;
import com.example.ckgl.dto.ProductionDayInDTO;
import com.example.ckgl.dto.ProductionEntryDTO;
import com.example.ckgl.dto.ProductionLedgerVO;
import com.example.ckgl.dto.ProductionOutRowVO;
import com.example.ckgl.dto.ProductionSettleDTO;
import com.example.ckgl.service.ProductionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 生产出入库：源头零件生产入库、尾端产线消耗、月度结算 */
@RestController
@RequestMapping("/api/production")
public class ProductionController {

    @Autowired
    private ProductionService productionService;

    /** 月度台账：上月库存、本月库存、每日出库数量 */
    @GetMapping("/ledger")
    public Result<ProductionLedgerVO> ledger(@RequestParam(required = false) String category,
                                             @RequestParam(required = false) String month) {
        return Result.ok(productionService.ledger(category, month));
    }

    /** 生产入库 */
    @PostMapping("/in")
    public Result<Void> addIn(@Valid @RequestBody ProductionEntryDTO dto) {
        productionService.addIn(dto);
        return Result.ok();
    }

    /** 直接设置某零件某天的入库数量（表格单元格编辑） */
    @PutMapping("/in")
    public Result<Void> setDayIn(@Valid @RequestBody ProductionDayInDTO dto) {
        productionService.setIn(dto.getPartId(), dto.getDate(), dto.getQuantity());
        return Result.ok();
    }

    /** 产线出库（当日累加） */
    @PostMapping("/out")
    public Result<Void> addOut(@Valid @RequestBody ProductionEntryDTO dto) {
        productionService.addOut(dto);
        return Result.ok();
    }

    /** 出库页：所选日期的用料汇总 + 当日已出库 */
    @GetMapping("/daily-summary")
    public Result<List<ProductionOutRowVO>> dailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(productionService.dailySummary(date != null ? date : LocalDate.now()));
    }

    /** 结算上月库存到所选月份期初，返回零件数 */
    @PostMapping("/settle")
    public Result<Integer> settle(@Valid @RequestBody ProductionSettleDTO dto) {
        return Result.ok(productionService.settle(dto.getMonth()));
    }
}
