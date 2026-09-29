package com.example.ckgl.controller;

import com.example.ckgl.common.BusinessException;
import com.example.ckgl.common.Result;
import com.example.ckgl.dto.StockItemVO;
import com.example.ckgl.service.StockTakeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 库存盘点：按货位核对物料与数量 */
@RestController
@RequestMapping("/api/stock-take")
public class StockTakeController {

    @Autowired
    private StockTakeService stockTakeService;

    /** 指定货位的全部物料（含数量为 0 的） */
    @GetMapping("/locations/{locationId}/items")
    public Result<List<StockItemVO>> items(@PathVariable Long locationId) {
        return Result.ok(stockTakeService.locationItems(locationId));
    }

    /** 直接修改某物料数量 */
    @PutMapping("/locations/{locationId}/items/{partId}")
    public Result<Void> setQuantity(@PathVariable Long locationId, @PathVariable Long partId,
                                    @RequestBody Map<String, Integer> body) {
        Integer quantity = body.get("quantity");
        if (quantity == null || quantity < 0) {
            throw new BusinessException("数量必须大于等于 0");
        }
        stockTakeService.setQuantity(locationId, partId, quantity);
        return Result.ok();
    }

    /** 一键清空该货位 */
    @DeleteMapping("/locations/{locationId}/items")
    public Result<Integer> clear(@PathVariable Long locationId) {
        return Result.ok(stockTakeService.clearLocation(locationId));
    }

    /** 增加物料（已存在则数量累加） */
    @PostMapping("/locations/{locationId}/items")
    public Result<Void> addPart(@PathVariable Long locationId, @Valid @RequestBody Map<String, Object> body) {
        Long partId = ((Number) body.get("partId")).longValue();
        Integer quantity = (Integer) body.get("quantity");
        stockTakeService.addPart(locationId, partId, quantity == null || quantity < 1 ? 1 : quantity);
        return Result.ok();
    }

    /** 删除某物料 */
    @DeleteMapping("/locations/{locationId}/items/{partId}")
    public Result<Void> removePart(@PathVariable Long locationId, @PathVariable Long partId) {
        stockTakeService.removePart(locationId, partId);
        return Result.ok();
    }
}
