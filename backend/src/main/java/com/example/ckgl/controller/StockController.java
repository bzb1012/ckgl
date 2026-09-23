package com.example.ckgl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.ckgl.common.Result;
import com.example.ckgl.dto.StockQuery;
import com.example.ckgl.dto.StockVO;
import com.example.ckgl.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping
    public Result<IPage<StockVO>> list(StockQuery query) {
        return Result.ok(stockService.pageStocks(query));
    }
}
