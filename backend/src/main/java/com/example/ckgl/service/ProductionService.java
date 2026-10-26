package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.ProductPartDetail;
import com.example.ckgl.dto.ProductionEntryDTO;
import com.example.ckgl.dto.ProductionLedgerItemVO;
import com.example.ckgl.dto.ProductionLedgerVO;
import com.example.ckgl.dto.ProductionOutExportVO;
import com.example.ckgl.dto.ProductionOutRowVO;
import com.example.ckgl.entity.Part;
import com.example.ckgl.entity.PartMonthSettle;
import com.example.ckgl.entity.PartProductionIn;
import com.example.ckgl.entity.PartProductionOut;
import com.example.ckgl.entity.ProductionPlan;
import com.example.ckgl.mapper.PartMapper;
import com.example.ckgl.mapper.PartMonthSettleMapper;
import com.example.ckgl.mapper.PartProductionInMapper;
import com.example.ckgl.mapper.PartProductionOutMapper;
import com.example.ckgl.mapper.ProductPartMapper;
import com.example.ckgl.mapper.ProductionPlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生产出入库台账：源头零件生产入库、尾端产线消耗、月度结算结转。
 * 独立台账，不影响仓库货位库存。
 */
@Service
public class ProductionService {

    @Autowired
    private PartMapper partMapper;
    @Autowired
    private ProductionPlanMapper productionPlanMapper;
    @Autowired
    private ProductPartMapper productPartMapper;
    @Autowired
    private PartProductionInMapper inMapper;
    @Autowired
    private PartProductionOutMapper outMapper;
    @Autowired
    private PartMonthSettleMapper settleMapper;

    /** 某月的台账表格：按分类取零件，返回上月库存、本月库存与每日消耗 */
    public ProductionLedgerVO ledger(String category, String month) {
        YearMonth ym = parseMonth(month);
        LocalDate first = ym.atDay(1);
        LocalDate next = ym.plusMonths(1).atDay(1);
        int days = ym.lengthOfMonth();

        LambdaQueryWrapper<Part> partWrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            partWrapper.eq(Part::getCategory, category);
        }
        partWrapper.orderByAsc(Part::getCode);
        List<Part> parts = partMapper.selectList(partWrapper);

        Map<Long, Integer> inBefore = sumIn(null, first);
        Map<Long, Integer> outBefore = sumOut(null, first);
        Map<Long, Integer> settledOpening = loadSettled(ym.toString());
        Map<Long, Map<Integer, Integer>> dailyIn = dailyIn(first, next);
        Map<Long, Map<Integer, Integer>> dailyOut = dailyOut(first, next);

        List<ProductionLedgerItemVO> items = new ArrayList<>();
        for (Part part : parts) {
            int opening = settledOpening.containsKey(part.getId())
                    ? settledOpening.get(part.getId())
                    : inBefore.getOrDefault(part.getId(), 0) - outBefore.getOrDefault(part.getId(), 0);

            List<Integer> dailyInList = new ArrayList<>(Collections.nCopies(days, 0));
            int inTotal = 0;
            for (Map.Entry<Integer, Integer> e : dailyIn.getOrDefault(part.getId(), Map.of()).entrySet()) {
                int day = e.getKey();
                if (day >= 1 && day <= days) {
                    dailyInList.set(day - 1, e.getValue());
                    inTotal += e.getValue();
                }
            }

            List<Integer> daily = new ArrayList<>(Collections.nCopies(days, 0));
            int outTotal = 0;
            for (Map.Entry<Integer, Integer> e : dailyOut.getOrDefault(part.getId(), Map.of()).entrySet()) {
                int day = e.getKey();
                if (day >= 1 && day <= days) {
                    daily.set(day - 1, e.getValue());
                    outTotal += e.getValue();
                }
            }

            ProductionLedgerItemVO vo = new ProductionLedgerItemVO();
            vo.setPartId(part.getId());
            vo.setPartCode(part.getCode());
            vo.setPartName(part.getName());
            vo.setUnit(part.getUnit());
            vo.setCategory(part.getCategory());
            vo.setOpening(opening);
            vo.setInTotal(inTotal);
            vo.setOutTotal(outTotal);
            vo.setCurrent(opening + inTotal - outTotal);
            vo.setDailyOut(daily);
            vo.setDailyIn(dailyInList);
            items.add(vo);
        }

        ProductionLedgerVO result = new ProductionLedgerVO();
        result.setMonth(ym.toString());
        result.setDays(days);
        result.setItems(items);
        return result;
    }

    /** 源头零件生产入库 */
    public void addIn(ProductionEntryDTO dto) {
        requirePart(dto.getPartId());
        PartProductionIn record = new PartProductionIn();
        record.setPartId(dto.getPartId());
        record.setInDate(dto.getDate());
        record.setQuantity(dto.getQuantity());
        record.setRemark(dto.getRemark());
        inMapper.insert(record);
    }

    /** 尾端产线消耗，多次录入按日累加 */
    public void addOut(ProductionEntryDTO dto) {
        requirePart(dto.getPartId());
        PartProductionOut record = new PartProductionOut();
        record.setPartId(dto.getPartId());
        record.setOutDate(dto.getDate());
        record.setQuantity(dto.getQuantity());
        record.setRemark(dto.getRemark());
        outMapper.insert(record);
    }

    /** 直接设置某零件某天的入库总量（表格单元格编辑，0 表示清除当天入库） */
    @Transactional(rollbackFor = Exception.class)
    public void setIn(Long partId, LocalDate date, int quantity) {
        requirePart(partId);
        inMapper.delete(new LambdaQueryWrapper<PartProductionIn>()
                .eq(PartProductionIn::getPartId, partId)
                .eq(PartProductionIn::getInDate, date));
        if (quantity > 0) {
            PartProductionIn record = new PartProductionIn();
            record.setPartId(partId);
            record.setInDate(date);
            record.setQuantity(quantity);
            inMapper.insert(record);
        }
    }

    /**
     * 产线出库页数据：所选日期的计划用料汇总 + 当日已出库合计。
     * 按计划数量统计，不考虑已完成数量；已完成计划与过去日期同样统计。
     */
    public List<ProductionOutRowVO> dailySummary(LocalDate date) {
        List<ProductionPlan> plans = productionPlanMapper.selectList(
                new LambdaQueryWrapper<ProductionPlan>().apply("plan_date = {0}", date));

        Map<Long, Integer> needMap = new HashMap<>();
        Map<Long, Integer> planCount = new HashMap<>();
        for (ProductionPlan plan : plans) {
            int quantity = plan.getQuantity();
            Set<Long> seen = new HashSet<>();
            for (ProductPartDetail pp : productPartMapper.selectByProductId(plan.getProductId())) {
                needMap.merge(pp.getPartId(), pp.getQuantity() * quantity, Integer::sum);
                if (seen.add(pp.getPartId())) {
                    planCount.merge(pp.getPartId(), 1, Integer::sum);
                }
            }
        }
        if (needMap.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> outToday = sumOut(date, date.plusDays(1));
        List<ProductionOutRowVO> rows = new ArrayList<>();
        for (Part part : partMapper.selectBatchIds(needMap.keySet())) {
            ProductionOutRowVO vo = new ProductionOutRowVO();
            vo.setPartId(part.getId());
            vo.setPartCode(part.getCode());
            vo.setPartName(part.getName());
            vo.setUnit(part.getUnit());
            vo.setTotalNeed(needMap.get(part.getId()));
            vo.setPlanCount(planCount.getOrDefault(part.getId(), 0));
            vo.setOutToday(outToday.getOrDefault(part.getId(), 0));
            rows.add(vo);
        }
        rows.sort(Comparator.comparing(ProductionOutRowVO::getPartCode));
        return rows;
    }

    /**
     * 产线出库导出：日期区间内逐日用料汇总，并给出区间合计。
     * 按计划数量统计，不考虑已完成数量。
     */
    public ProductionOutExportVO outExport(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        List<ProductionPlan> plans = productionPlanMapper.selectList(
                new LambdaQueryWrapper<ProductionPlan>()
                        .ge(ProductionPlan::getPlanDate, start)
                        .le(ProductionPlan::getPlanDate, end));

        Map<LocalDate, Map<Long, Integer>> needByDate = new HashMap<>();
        Map<LocalDate, Map<Long, Integer>> countByDate = new HashMap<>();
        Set<Long> partIds = new HashSet<>();
        for (ProductionPlan plan : plans) {
            Map<Long, Integer> need = needByDate.computeIfAbsent(plan.getPlanDate(), k -> new HashMap<>());
            Map<Long, Integer> count = countByDate.computeIfAbsent(plan.getPlanDate(), k -> new HashMap<>());
            Set<Long> seen = new HashSet<>();
            for (ProductPartDetail pp : productPartMapper.selectByProductId(plan.getProductId())) {
                need.merge(pp.getPartId(), pp.getQuantity() * plan.getQuantity(), Integer::sum);
                if (seen.add(pp.getPartId())) {
                    count.merge(pp.getPartId(), 1, Integer::sum);
                }
                partIds.add(pp.getPartId());
            }
        }

        Map<Long, Part> partMap = new HashMap<>();
        if (!partIds.isEmpty()) {
            for (Part part : partMapper.selectBatchIds(partIds)) {
                partMap.put(part.getId(), part);
            }
        }

        Map<LocalDate, Map<Long, Integer>> outByDate = outTotalsByDate(start, end.plusDays(1));
        List<ProductionOutExportVO.DaySheet> days = new ArrayList<>();
        Map<Long, Integer> totalNeed = new HashMap<>();
        Map<Long, Integer> totalCount = new HashMap<>();
        Map<Long, Integer> totalOut = new HashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<Long, Integer> need = needByDate.getOrDefault(d, Map.of());
            Map<Long, Integer> count = countByDate.getOrDefault(d, Map.of());
            Map<Long, Integer> out = outByDate.getOrDefault(d, Map.of());

            ProductionOutExportVO.DaySheet sheet = new ProductionOutExportVO.DaySheet();
            sheet.setDate(d.toString());
            sheet.setRows(exportRows(need, count, out, partMap));
            days.add(sheet);

            need.forEach((k, v) -> totalNeed.merge(k, v, Integer::sum));
            count.forEach((k, v) -> totalCount.merge(k, v, Integer::sum));
            out.forEach((k, v) -> totalOut.merge(k, v, Integer::sum));
        }

        ProductionOutExportVO result = new ProductionOutExportVO();
        result.setDays(days);
        result.setTotal(exportRows(totalNeed, totalCount, totalOut, partMap));
        return result;
    }

    private List<ProductionOutExportVO.DailyRow> exportRows(Map<Long, Integer> need,
                                                            Map<Long, Integer> count,
                                                            Map<Long, Integer> out,
                                                            Map<Long, Part> partMap) {
        List<ProductionOutExportVO.DailyRow> rows = new ArrayList<>();
        for (Long partId : need.keySet()) {
            Part part = partMap.get(partId);
            if (part == null) {
                continue;
            }
            ProductionOutExportVO.DailyRow row = new ProductionOutExportVO.DailyRow();
            row.setPartCode(part.getCode());
            row.setPartName(part.getName());
            row.setUnit(part.getUnit());
            row.setTotalNeed(need.getOrDefault(partId, 0));
            row.setPlanCount(count.getOrDefault(partId, 0));
            row.setOutTotal(out.getOrDefault(partId, 0));
            rows.add(row);
        }
        rows.sort(Comparator.comparing(ProductionOutExportVO.DailyRow::getPartCode));
        return rows;
    }

    /** 按日期+零件汇总出库数量，[from, to) */
    private Map<LocalDate, Map<Long, Integer>> outTotalsByDate(LocalDate from, LocalDate to) {
        QueryWrapper<PartProductionOut> qw = new QueryWrapper<PartProductionOut>()
                .select("out_date AS outDate", "part_id AS partId", "IFNULL(SUM(quantity),0) AS total")
                .ge("out_date", from)
                .lt("out_date", to)
                .groupBy("out_date", "part_id");
        Map<LocalDate, Map<Long, Integer>> result = new HashMap<>();
        for (Map<String, Object> row : outMapper.selectMaps(qw)) {
            LocalDate date = LocalDate.parse(row.get("outDate").toString());
            Long partId = ((Number) row.get("partId")).longValue();
            int total = ((Number) row.get("total")).intValue();
            result.computeIfAbsent(date, k -> new HashMap<>()).put(partId, total);
        }
        return result;
    }

    /**
     * 结算：把上月期末（期初 + 入库 - 消耗）结转为本月期初，覆盖式写入。
     * 返回参与结算的零件数。
     */
    @Transactional(rollbackFor = Exception.class)
    public int settle(String month) {
        YearMonth target = parseMonth(month);
        YearMonth prev = target.minusMonths(1);
        LocalDate prevFirst = prev.atDay(1);
        LocalDate targetFirst = target.atDay(1);

        Map<Long, Integer> prevSettled = loadSettled(prev.toString());
        Map<Long, Integer> inBeforePrev = sumIn(null, prevFirst);
        Map<Long, Integer> outBeforePrev = sumOut(null, prevFirst);
        Map<Long, Integer> inPrev = sumIn(prevFirst, targetFirst);
        Map<Long, Integer> outPrev = sumOut(prevFirst, targetFirst);

        List<Part> parts = partMapper.selectList(null);
        settleMapper.delete(new LambdaQueryWrapper<PartMonthSettle>()
                .eq(PartMonthSettle::getMonthKey, target.toString()));
        for (Part part : parts) {
            int openingPrev = prevSettled.containsKey(part.getId())
                    ? prevSettled.get(part.getId())
                    : inBeforePrev.getOrDefault(part.getId(), 0) - outBeforePrev.getOrDefault(part.getId(), 0);
            int closing = openingPrev + inPrev.getOrDefault(part.getId(), 0) - outPrev.getOrDefault(part.getId(), 0);
            PartMonthSettle settle = new PartMonthSettle();
            settle.setPartId(part.getId());
            settle.setMonthKey(target.toString());
            settle.setOpeningQty(closing);
            settleMapper.insert(settle);
        }
        return parts.size();
    }

    /** 按零件汇总入库数量，[from, to) */
    private Map<Long, Integer> sumIn(LocalDate from, LocalDate to) {
        QueryWrapper<PartProductionIn> qw = new QueryWrapper<PartProductionIn>()
                .select("part_id AS partId", "IFNULL(SUM(quantity),0) AS total")
                .groupBy("part_id");
        if (from != null) {
            qw.ge("in_date", from);
        }
        if (to != null) {
            qw.lt("in_date", to);
        }
        return toIntMap(inMapper.selectMaps(qw));
    }

    /** 按零件汇总出库数量，[from, to) */
    private Map<Long, Integer> sumOut(LocalDate from, LocalDate to) {
        QueryWrapper<PartProductionOut> qw = new QueryWrapper<PartProductionOut>()
                .select("part_id AS partId", "IFNULL(SUM(quantity),0) AS total")
                .groupBy("part_id");
        if (from != null) {
            qw.ge("out_date", from);
        }
        if (to != null) {
            qw.lt("out_date", to);
        }
        return toIntMap(outMapper.selectMaps(qw));
    }

    /** 每日入库合计：partId -> (日, 数量) */
    private Map<Long, Map<Integer, Integer>> dailyIn(LocalDate from, LocalDate to) {
        QueryWrapper<PartProductionIn> qw = new QueryWrapper<PartProductionIn>()
                .select("part_id AS partId", "DAY(in_date) AS dayNo", "IFNULL(SUM(quantity),0) AS total")
                .ge("in_date", from)
                .lt("in_date", to)
                .groupBy("part_id", "DAY(in_date)");
        Map<Long, Map<Integer, Integer>> result = new HashMap<>();
        for (Map<String, Object> row : inMapper.selectMaps(qw)) {
            Long partId = ((Number) row.get("partId")).longValue();
            int day = ((Number) row.get("dayNo")).intValue();
            int total = ((Number) row.get("total")).intValue();
            result.computeIfAbsent(partId, k -> new HashMap<>()).put(day, total);
        }
        return result;
    }

    /** 每日出库合计：partId -> (日, 数量) */
    private Map<Long, Map<Integer, Integer>> dailyOut(LocalDate from, LocalDate to) {
        QueryWrapper<PartProductionOut> qw = new QueryWrapper<PartProductionOut>()
                .select("part_id AS partId", "DAY(out_date) AS dayNo", "IFNULL(SUM(quantity),0) AS total")
                .ge("out_date", from)
                .lt("out_date", to)
                .groupBy("part_id", "DAY(out_date)");
        Map<Long, Map<Integer, Integer>> result = new HashMap<>();
        for (Map<String, Object> row : outMapper.selectMaps(qw)) {
            Long partId = ((Number) row.get("partId")).longValue();
            int day = ((Number) row.get("dayNo")).intValue();
            int total = ((Number) row.get("total")).intValue();
            result.computeIfAbsent(partId, k -> new HashMap<>()).put(day, total);
        }
        return result;
    }

    private Map<Long, Integer> loadSettled(String monthKey) {
        Map<Long, Integer> map = new HashMap<>();
        for (PartMonthSettle settle : settleMapper.selectList(new LambdaQueryWrapper<PartMonthSettle>()
                .eq(PartMonthSettle::getMonthKey, monthKey))) {
            map.put(settle.getPartId(), settle.getOpeningQty());
        }
        return map;
    }

    private Map<Long, Integer> toIntMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(((Number) row.get("partId")).longValue(), ((Number) row.get("total")).intValue());
        }
        return map;
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException("月份格式应为 YYYY-MM");
        }
    }

    private void requirePart(Long partId) {
        if (partMapper.selectById(partId) == null) {
            throw new BusinessException("零件不存在");
        }
    }
}
