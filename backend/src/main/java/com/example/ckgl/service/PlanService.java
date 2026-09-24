package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.PlanAlertVO;
import com.example.ckgl.dto.PlanCompletedDTO;
import com.example.ckgl.dto.PlanDTO;
import com.example.ckgl.dto.PlanPartVO;
import com.example.ckgl.dto.PlanVO;
import com.example.ckgl.dto.ProductPartDetail;
import com.example.ckgl.dto.TodayPartVO;
import com.example.ckgl.entity.Part;
import com.example.ckgl.entity.ProductionPlan;
import com.example.ckgl.entity.Product;
import com.example.ckgl.entity.Stock;
import com.example.ckgl.mapper.PartMapper;
import com.example.ckgl.mapper.ProductionPlanMapper;
import com.example.ckgl.mapper.ProductMapper;
import com.example.ckgl.mapper.ProductPartMapper;
import com.example.ckgl.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlanService extends ServiceImpl<ProductionPlanMapper, ProductionPlan> {

    /** 安全库存余量：库存需大于(需求+100)，否则预警 */
    public static final int SAFETY_STOCK = 100;

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPartMapper productPartMapper;
    @Autowired
    private PartMapper partMapper;
    @Autowired
    private StockMapper stockMapper;

    /** 未完成计划条件：completed < quantity */
    private LambdaQueryWrapper<ProductionPlan> undoneWrapper() {
        return new LambdaQueryWrapper<ProductionPlan>().apply("completed < quantity");
    }

    public Page<PlanVO> page(String line, LocalDate date, boolean onlyUndone, long page, long size) {
        LambdaQueryWrapper<ProductionPlan> wrapper = new LambdaQueryWrapper<>();
        if (line != null && !line.isBlank()) {
            // 产线标签：看该线指定日期（默认今天）的计划
            wrapper.eq(ProductionPlan::getLine, line)
                    .apply("plan_date = {0}", date != null ? date : LocalDate.now());
        }
        if (onlyUndone) {
            wrapper.apply("completed < quantity");
        }
        wrapper.orderByAsc(ProductionPlan::getPlanDate).orderByAsc(ProductionPlan::getId);
        Page<ProductionPlan> result = this.page(new Page<>(page, size), wrapper);

        Map<Long, Integer> demandMap = aggregateDemand();
        Map<Long, Integer> stockMap = aggregateStock();
        Set<Long> productIds = new HashSet<>();
        result.getRecords().forEach(p -> productIds.add(p.getProductId()));
        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                        .collect(HashMap::new, (m, p) -> m.put(p.getId(), p), HashMap::putAll);

        List<PlanVO> vos = new ArrayList<>();
        for (ProductionPlan plan : result.getRecords()) {
            PlanVO vo = new PlanVO();
            vo.setId(plan.getId());
            vo.setPlanDate(plan.getPlanDate());
            vo.setLine(plan.getLine());
            vo.setProductId(plan.getProductId());
            Product product = productMap.get(plan.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode());
                vo.setProductName(product.getName());
            }
            vo.setQuantity(plan.getQuantity());
            vo.setCompleted(plan.getCompleted());
            vo.setRemark(plan.getRemark());
            List<PlanPartVO> parts = partsOf(plan, demandMap, stockMap);
            vo.setParts(parts);
            if (plan.getCompleted() >= plan.getQuantity()) {
                // 已完成的计划不再消耗零件，不参与缺料判断，避免预警误报
                vo.setShortages(List.of());
                vo.setShortage(false);
            } else {
                vo.setShortages(parts.stream().filter(p -> !p.getEnough()).toList());
                vo.setShortage(!vo.getShortages().isEmpty());
            }
            vos.add(vo);
        }
        Page<PlanVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    /** 库存预警汇总：按零件聚合全部未完成计划的需求，与当前库存对比 */
    public List<PlanAlertVO> alerts() {
        Map<Long, Integer> demandMap = aggregateDemand();
        if (demandMap.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> stockMap = aggregateStock();

        List<ProductionPlan> undone = this.list(undoneWrapper());
        Map<Long, Integer> planCount = new HashMap<>();
        for (ProductionPlan plan : undone) {
            Set<Long> seen = new HashSet<>();
            for (ProductPartDetail pp : productPartMapper.selectByProductId(plan.getProductId())) {
                // 同一计划内同一零件只计一次
                if (seen.add(pp.getPartId())) {
                    planCount.merge(pp.getPartId(), 1, Integer::sum);
                }
            }
        }

        List<PlanAlertVO> alerts = new ArrayList<>();
        List<Part> parts = partMapper.selectBatchIds(demandMap.keySet());
        for (Part part : parts) {
            int demand = demandMap.getOrDefault(part.getId(), 0);
            int stock = stockMap.getOrDefault(part.getId(), 0);
            // 预警规则：库存需大于(需求 + 安全库存100)，否则预警
            if (stock < demand + SAFETY_STOCK) {
                PlanAlertVO vo = new PlanAlertVO();
                vo.setPartId(part.getId());
                vo.setPartCode(part.getCode());
                vo.setPartName(part.getName());
                vo.setUnit(part.getUnit());
                vo.setDemand(demand);
                vo.setStock(stock);
                vo.setSafetyStock(SAFETY_STOCK);
                vo.setLack(demand + SAFETY_STOCK - stock);
                vo.setPlanCount(planCount.getOrDefault(part.getId(), 0));
                alerts.add(vo);
            }
        }
        alerts.sort(Comparator.comparingInt(PlanAlertVO::getLack).reversed());
        return alerts;
    }

    /**
     * 指定日期的用料汇总：该日期未完成计划的剩余需求，按零件合并、数量累加。
     * 已完成的计划不再消耗零件，不参与统计（与库存预警口径一致）。
     */
    public List<TodayPartVO> todayParts(LocalDate date) {
        List<ProductionPlan> plans = this.list(new LambdaQueryWrapper<ProductionPlan>()
                .apply("plan_date = {0}", date)
                .apply("completed < quantity"));
        Map<Long, Integer> needMap = new HashMap<>();
        Map<Long, Integer> planCount = new HashMap<>();
        for (ProductionPlan plan : plans) {
            int remaining = plan.getQuantity() - plan.getCompleted();
            Set<Long> seen = new HashSet<>();
            for (ProductPartDetail pp : productPartMapper.selectByProductId(plan.getProductId())) {
                needMap.merge(pp.getPartId(), pp.getQuantity() * remaining, Integer::sum);
                // 同一计划内同一零件只计一次计划数
                if (seen.add(pp.getPartId())) {
                    planCount.merge(pp.getPartId(), 1, Integer::sum);
                }
            }
        }
        if (needMap.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> stockMap = aggregateStock();
        List<TodayPartVO> result = new ArrayList<>();
        for (Part part : partMapper.selectBatchIds(needMap.keySet())) {
            TodayPartVO vo = new TodayPartVO();
            vo.setPartId(part.getId());
            vo.setPartCode(part.getCode());
            vo.setPartName(part.getName());
            vo.setUnit(part.getUnit());
            vo.setTotalNeed(needMap.get(part.getId()));
            vo.setPlanCount(planCount.getOrDefault(part.getId(), 0));
            vo.setStock(stockMap.getOrDefault(part.getId(), 0));
            result.add(vo);
        }
        result.sort(Comparator.comparing(TodayPartVO::getPartCode));
        return result;
    }

    /** 产线选项：固定 1/2/3线 + 数据中已出现的其他产线，按编号排序 */
    public List<String> lines() {
        Set<String> set = new LinkedHashSet<>(List.of("1线", "2线", "3线"));
        this.list(new LambdaQueryWrapper<ProductionPlan>().select(ProductionPlan::getLine)).stream()
                .map(ProductionPlan::getLine)
                .filter(l -> l != null && !l.isBlank())
                .forEach(set::add);
        List<String> list = new ArrayList<>(set);
        list.sort(Comparator.comparingInt(PlanService::lineNum).thenComparing(Comparator.naturalOrder()));
        return list;
    }

    private static int lineNum(String s) {
        Matcher m = Pattern.compile("(\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    public ProductionPlan create(PlanDTO dto) {
        if (productMapper.selectById(dto.getProductId()) == null) {
            throw new BusinessException("产品不存在");
        }
        ProductionPlan plan = new ProductionPlan();
        plan.setPlanDate(dto.getPlanDate());
        plan.setLine(dto.getLine().trim());
        plan.setProductId(dto.getProductId());
        plan.setQuantity(dto.getQuantity());
        plan.setCompleted(0);
        plan.setRemark(dto.getRemark());
        this.save(plan);
        return plan;
    }

    public void update(Long id, PlanDTO dto) {
        ProductionPlan plan = this.getById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (productMapper.selectById(dto.getProductId()) == null) {
            throw new BusinessException("产品不存在");
        }
        plan.setPlanDate(dto.getPlanDate());
        plan.setLine(dto.getLine().trim());
        plan.setProductId(dto.getProductId());
        plan.setQuantity(dto.getQuantity());
        plan.setRemark(dto.getRemark());
        if (plan.getCompleted() > plan.getQuantity()) {
            throw new BusinessException("已完成数量不能大于计划数量，请先调整已完成数量");
        }
        this.updateById(plan);
    }

    /** 实时更新已完成数量（用于避免预警误报） */
    public void updateCompleted(Long id, PlanCompletedDTO dto) {
        ProductionPlan plan = this.getById(id);
        if (plan == null) {
            throw new BusinessException("计划不存在");
        }
        if (dto.getCompleted() > plan.getQuantity()) {
            throw new BusinessException("已完成数量不能大于计划数量 " + plan.getQuantity());
        }
        plan.setCompleted(dto.getCompleted());
        this.updateById(plan);
    }

    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("计划不存在");
        }
        this.removeById(id);
    }

    /** 全部未完成计划的零件总需求：Σ(计划数量-已完成)×单件用量 */
    private Map<Long, Integer> aggregateDemand() {
        Map<Long, Integer> demand = new HashMap<>();
        for (ProductionPlan plan : this.list(undoneWrapper())) {
            int remaining = plan.getQuantity() - plan.getCompleted();
            for (ProductPartDetail pp : productPartMapper.selectByProductId(plan.getProductId())) {
                demand.merge(pp.getPartId(), pp.getQuantity() * remaining, Integer::sum);
            }
        }
        return demand;
    }

    /** 当前库存：全部货位按零件合计 */
    private Map<Long, Integer> aggregateStock() {
        QueryWrapper<Stock> qw = new QueryWrapper<Stock>()
                .select("part_id AS partId", "IFNULL(SUM(quantity),0) AS total")
                .groupBy("part_id");
        Map<Long, Integer> stock = new HashMap<>();
        for (Map<String, Object> row : stockMapper.selectMaps(qw)) {
            stock.put(((Number) row.get("partId")).longValue(), ((Number) row.get("total")).intValue());
        }
        return stock;
    }

    /** 计划关联全部零件的库存对比：含足够与不足的零件 */
    private List<PlanPartVO> partsOf(ProductionPlan plan, Map<Long, Integer> demandMap, Map<Long, Integer> stockMap) {
        int remaining = plan.getQuantity() - plan.getCompleted();
        List<PlanPartVO> list = new ArrayList<>();
        for (ProductPartDetail pp : productPartMapper.selectByProductId(plan.getProductId())) {
            int demand = demandMap.getOrDefault(pp.getPartId(), 0);
            int stock = stockMap.getOrDefault(pp.getPartId(), 0);
            int lack = demand + SAFETY_STOCK - stock;
            PlanPartVO vo = new PlanPartVO();
            vo.setPartId(pp.getPartId());
            vo.setPartCode(pp.getPartCode());
            vo.setPartName(pp.getPartName());
            vo.setUnit(pp.getUnit());
            vo.setUsage(pp.getQuantity());
            vo.setNeed(pp.getQuantity() * remaining);
            vo.setDemand(demand);
            vo.setSafetyStock(SAFETY_STOCK);
            vo.setStock(stock);
            vo.setLack(lack);
            vo.setEnough(lack <= 0);
            list.add(vo);
        }
        return list;
    }
}
