package com.example.ckgl.task;

import com.example.ckgl.dto.ProductionConfigVO;
import com.example.ckgl.service.ProductionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 产线定时出库：每天 8:00~20:00 按配置间隔（默认 30 分钟）执行一次，
 * 每条产线按生产计划完成指定数量，零件按 BOM 折算出库，并检查库存预警
 * （每次执行都推送，不做去重）。
 * 实现：每分钟 tick，距上次执行满配置间隔分钟才真正执行，间隔改动即时生效。
 */
@Component
public class ProductionAutoTask {

    private static final Logger log = LoggerFactory.getLogger(ProductionAutoTask.class);

    @Autowired
    private ProductionService productionService;

    /** 上次执行时间（内存记录，重启后重新起算） */
    private volatile LocalDateTime lastRun;

    @Scheduled(cron = "0 * 8-20 * * *", zone = "Asia/Shanghai")
    public void tick() {
        try {
            ProductionConfigVO cfg = productionService.getConfig();
            LocalDateTime now = LocalDateTime.now();
            if (lastRun != null && Duration.between(lastRun, now).toMinutes() < cfg.getIntervalMin()) {
                return;
            }
            lastRun = now;
            var result = productionService.autoOut();
            if ((int) result.get("plans") > 0) {
                log.info("定时出库完成: 推进产线 {}, 计划 {} 条, 出库零件 {} 种",
                        result.get("line"), result.get("plans"), result.get("outKinds"));
            }
        } catch (Exception e) {
            log.error("定时出库执行失败", e);
        }
    }
}
