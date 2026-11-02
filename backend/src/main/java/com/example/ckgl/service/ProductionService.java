package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.ProductPartDetail;
import com.example.ckgl.dto.OpeningImportDTO;
import com.example.ckgl.dto.OpeningImportVO;
import com.example.ckgl.dto.ProductionConfigDTO;
import com.example.ckgl.dto.ProductionConfigVO;
import com.example.ckgl.dto.ProductionEntryDTO;
import com.example.ckgl.dto.ProductionLedgerItemVO;
import com.example.ckgl.dto.ProductionLedgerVO;
import com.example.ckgl.dto.ProductionOutExportVO;
import com.example.ckgl.dto.ProductionOutRowVO;
import com.example.ckgl.entity.Part;
import com.example.ckgl.entity.PartMonthSettle;
import com.example.ckgl.entity.PartProductionIn;
import com.example.ckgl.entity.PartProductionOut;
import com.example.ckgl.entity.ProductionConfig;
import com.example.ckgl.entity.ProductionPlan;
import com.example.ckgl.mapper.PartMapper;
import com.example.ckgl.mapper.PartMonthSettleMapper;
import com.example.ckgl.mapper.PartProductionInMapper;
import com.example.ckgl.mapper.PartProductionOutMapper;
import com.example.ckgl.mapper.ProductPartMapper;
import com.example.ckgl.mapper.ProductionConfigMapper;
import com.example.ckgl.mapper.ProductionPlanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生产出入库台账：源头零件生产入库、尾端产线消耗、月度结算结转。
 * 独立台账，不影响仓库货位库存。
 */
@Service
public class ProductionService {

    private static final Logger log = LoggerFactory.getLogger(ProductionService.class);

    /** 配置默认值：每 30 分钟每线完成 1000 个计划产品，预警阈值 4000，Bark 未配置 */
    private static final int DEF_INTERVAL_QTY = 1000;
    private static final int DEF_INTERVAL_MIN = 30;
    private static final int DEF_THRESHOLD = 4000;
    private static final String KEY_INTERVAL_QTY = "intervalQty";
    private static final String KEY_INTERVAL_MIN = "intervalMin";
    private static final String KEY_THRESHOLD = "alertThreshold";
    private static final String KEY_BARK = "barkUrl";

    /** 预警去重：同一天同一零件只推送一次（定时任务用，重启后重置） */
    private static final Set<String> ALERTED_TODAY = ConcurrentHashMap.newKeySet();

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
    @Autowired
    private ProductionConfigMapper configMapper;

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

    /**
     * 导入覆盖所选月份的上月库存（期初）：按零件型号匹配，
     * 匹配到的写/覆盖 part_month_settle，未匹配的型号在结果中返回。
     */
    @Transactional(rollbackFor = Exception.class)
    public OpeningImportVO importOpening(OpeningImportDTO dto) {
        YearMonth ym = parseMonth(dto.getMonth());
        String monthKey = ym.toString();

        Map<String, Part> byCode = new HashMap<>();
        for (Part part : partMapper.selectList(null)) {
            byCode.put(part.getCode(), part);
        }

        OpeningImportVO vo = new OpeningImportVO();
        for (OpeningImportDTO.Row row : dto.getItems()) {
            String code = row.getPartCode() == null ? "" : row.getPartCode().trim();
            if (code.isEmpty()) {
                continue;
            }
            Part part = byCode.get(code);
            if (part == null) {
                vo.getNotFound().add(code);
                continue;
            }
            int qty = row.getQuantity() == null ? 0 : Math.max(0, row.getQuantity());
            PartMonthSettle settle = settleMapper.selectOne(new LambdaQueryWrapper<PartMonthSettle>()
                    .eq(PartMonthSettle::getMonthKey, monthKey)
                    .eq(PartMonthSettle::getPartId, part.getId()));
            if (settle == null) {
                settle = new PartMonthSettle();
                settle.setPartId(part.getId());
                settle.setMonthKey(monthKey);
                settle.setOpeningQty(qty);
                settleMapper.insert(settle);
            } else {
                settle.setOpeningQty(qty);
                settleMapper.updateById(settle);
            }
            vo.setUpdated(vo.getUpdated() + 1);
        }
        return vo;
    }

    /* ==================== 定时出库 + 库存预警 + Bark 通知 ==================== */

    /** 读取配置（未配置项用默认值） */
    public ProductionConfigVO getConfig() {
        ProductionConfigVO vo = new ProductionConfigVO();
        vo.setIntervalQty(getConfigInt(KEY_INTERVAL_QTY, DEF_INTERVAL_QTY));
        vo.setIntervalMin(getConfigInt(KEY_INTERVAL_MIN, DEF_INTERVAL_MIN));
        vo.setAlertThreshold(getConfigInt(KEY_THRESHOLD, DEF_THRESHOLD));
        vo.setBarkUrl(getConfigStr(KEY_BARK, ""));
        return vo;
    }

    /** 保存配置（upsert，非法值回退默认） */
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ProductionConfigDTO dto) {
        upsert(KEY_INTERVAL_QTY, dto.getIntervalQty() == null ? String.valueOf(DEF_INTERVAL_QTY)
                : String.valueOf(Math.max(1, dto.getIntervalQty())));
        upsert(KEY_INTERVAL_MIN, dto.getIntervalMin() == null ? String.valueOf(DEF_INTERVAL_MIN)
                : String.valueOf(Math.max(5, dto.getIntervalMin())));
        upsert(KEY_THRESHOLD, dto.getAlertThreshold() == null ? String.valueOf(DEF_THRESHOLD)
                : String.valueOf(Math.max(0, dto.getAlertThreshold())));
        upsert(KEY_BARK, dto.getBarkUrl() == null ? "" : dto.getBarkUrl().trim());
    }

    /**
     * 定时出库（按生产计划的产线计划推进）：
     * 每条产线每个周期完成 intervalQty 个计划产品（封顶计划数量，做完自动停），
     * 完成数量累加到计划，零件按 BOM 用量折算合并出库，完成后检查库存预警。
     * 返回 {plans: 推进计划数, outKinds: 出库零件种类数, alerts: 预警消息}
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoOut(boolean dedupeAlert) {
        LocalDate today = LocalDate.now();
        ProductionConfigVO cfg = getConfig();

        List<ProductionPlan> plans = productionPlanMapper.selectList(
                new LambdaQueryWrapper<ProductionPlan>().apply("plan_date = {0}", today));
        Map<Long, Integer> partOut = new HashMap<>();
        int progressed = 0;
        for (ProductionPlan plan : plans) {
            int done = plan.getCompleted() == null ? 0 : plan.getCompleted();
            int progress = Math.min(cfg.getIntervalQty(), plan.getQuantity() - done);
            if (progress <= 0) {
                continue;
            }
            // 本周期完成量累加到计划（封顶计划数量）
            plan.setCompleted(done + progress);
            plan.setUpdatedAt(java.time.LocalDateTime.now());
            productionPlanMapper.updateById(plan);
            progressed++;
            // 按 BOM 用量折算零件消耗（跨计划合并）
            for (ProductPartDetail pp : productPartMapper.selectByProductId(plan.getProductId())) {
                partOut.merge(pp.getPartId(), pp.getQuantity() * progress, Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> entry : partOut.entrySet()) {
            PartProductionOut record = new PartProductionOut();
            record.setPartId(entry.getKey());
            record.setOutDate(today);
            record.setQuantity(entry.getValue());
            record.setRemark("定时出库(产线)");
            outMapper.insert(record);
        }
        List<String> alerts = checkAndAlert(cfg, dedupeAlert);
        Map<String, Object> result = new HashMap<>();
        result.put("plans", progressed);
        result.put("outKinds", partOut.size());
        result.put("alerts", alerts);
        return result;
    }

    /**
     * 库存预警：当天计划需要的零件中，本月库存低于阈值的收集为消息并合并推送 Bark。
     * dedupe=true 时同一天同一零件只推送一次。
     */
    public List<String> checkAndAlert(ProductionConfigVO cfg, boolean dedupe) {
        LocalDate today = LocalDate.now();
        List<ProductionOutRowVO> rows = dailySummary(today);
        List<String> alerts = new ArrayList<>();
        if (rows.isEmpty()) {
            return alerts;
        }
        // 本月库存（current = 上月库存 + 本月入库 − 本月出库）
        ProductionLedgerVO ledger = ledger(null, YearMonth.now().toString());
        Map<Long, Integer> currentMap = new HashMap<>();
        for (ProductionLedgerItemVO item : ledger.getItems()) {
            currentMap.put(item.getPartId(), item.getCurrent());
        }
        for (ProductionOutRowVO row : rows) {
            if (dedupe && !ALERTED_TODAY.add(today + ":" + row.getPartId())) {
                continue;
            }
            int current = currentMap.getOrDefault(row.getPartId(), 0);
            if (current < cfg.getAlertThreshold()) {
                alerts.add(row.getPartCode() + " 本月库存 " + current + "，今日需 " + row.getTotalNeed()
                        + "，缺 " + Math.max(0, row.getTotalNeed() - current));
            }
        }
        if (!alerts.isEmpty()) {
            String body = String.join("；", alerts);
            boolean sent = sendBark("生产库存预警", body);
            log.info("库存预警 {}: sent={}, {}", today, sent, body);
        }
        return alerts;
    }

    /** 发送 Bark 测试通知，返回结果消息 */
    public String testBark() {
        String base = getConfigStr(KEY_BARK, "");
        if (base.isBlank()) {
            return "未配置 Bark 推送地址";
        }
        return sendBark("仓库管理系统", "Bark 通知测试成功") ? "发送成功，请查看手机通知"
                : "发送失败，请检查 Bark 地址";
    }

    /** Bark 推送：GET {barkUrl}/{title}/{body}，失败仅记日志不影响业务 */
    private boolean sendBark(String title, String body) {
        String base = getConfigStr(KEY_BARK, "");
        if (base.isBlank()) {
            return false;
        }
        String url = (base.endsWith("/") ? base : base + "/")
                + URLEncoder.encode(title, StandardCharsets.UTF_8) + "/"
                + URLEncoder.encode(body, StandardCharsets.UTF_8);
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Bark 推送失败: {}", e.getMessage());
            return false;
        }
    }

    private int getConfigInt(String key, int def) {
        String val = getConfigStr(key, null);
        try {
            return val == null ? def : Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String getConfigStr(String key, String def) {
        ProductionConfig row = configMapper.selectById(key);
        return row == null || row.getConfigValue() == null ? def : row.getConfigValue();
    }

    private void upsert(String key, String value) {
        ProductionConfig row = configMapper.selectById(key);
        if (row == null) {
            row = new ProductionConfig();
            row.setConfigKey(key);
            row.setConfigValue(value);
            configMapper.insert(row);
        } else {
            row.setConfigValue(value);
            configMapper.updateById(row);
        }
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
