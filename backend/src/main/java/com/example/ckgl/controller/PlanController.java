package com.example.ckgl.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ckgl.common.Result;
import com.example.ckgl.dto.PlanAlertVO;
import com.example.ckgl.dto.PlanCompletedDTO;
import com.example.ckgl.dto.PlanDTO;
import com.example.ckgl.dto.PlanVO;
import com.example.ckgl.dto.TodayPartVO;
import com.example.ckgl.entity.ProductionPlan;
import com.example.ckgl.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private PlanService planService;

    @GetMapping
    public Result<Page<PlanVO>> list(@RequestParam(required = false) String line,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(defaultValue = "false") boolean onlyUndone,
                                     @RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size) {
        return Result.ok(planService.page(line, date, onlyUndone, page, size));
    }

    /** 产线选项（顶部快捷标签） */
    @GetMapping("/lines")
    public Result<List<String>> lines() {
        return Result.ok(planService.lines());
    }

    @GetMapping("/alerts")
    public Result<List<PlanAlertVO>> alerts() {
        return Result.ok(planService.alerts());
    }

    /** 用料汇总：指定日期（默认今天）未完成计划所需零件，同型号合并、数量累加 */
    @GetMapping("/today-parts")
    public Result<List<TodayPartVO>> todayParts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(planService.todayParts(date != null ? date : LocalDate.now()));
    }

    @PostMapping
    public Result<ProductionPlan> create(@Valid @RequestBody PlanDTO dto) {
        return Result.ok(planService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PlanDTO dto) {
        planService.update(id, dto);
        return Result.ok();
    }

    /** 实时更新已完成数量 */
    @PutMapping("/{id}/completed")
    public Result<Void> updateCompleted(@PathVariable Long id, @Valid @RequestBody PlanCompletedDTO dto) {
        planService.updateCompleted(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return Result.ok();
    }
}
