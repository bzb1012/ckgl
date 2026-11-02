package com.example.ckgl.task;

import com.example.ckgl.service.ProductionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 产线定时出库：每天 8:00~20:00 每小时整点执行一次，
 * 对当天生产计划涉及的零件按配置数量出库，并检查库存预警（同一天同一零件只推送一次）。
 */
@Component
public class ProductionAutoTask {

    private static final Logger log = LoggerFactory.getLogger(ProductionAutoTask.class);

    @Autowired
    private ProductionService productionService;

    @Scheduled(cron = "0 0 8-20 * * *", zone = "Asia/Shanghai")
    public void hourlyOut() {
        try {
            var result = productionService.autoOut(true);
            if ((int) result.get("parts") > 0) {
                log.info("定时出库完成: 零件 {} 个, 每轮 {}", result.get("parts"), result.get("perRound"));
            }
        } catch (Exception e) {
            log.error("定时出库执行失败", e);
        }
    }
}
