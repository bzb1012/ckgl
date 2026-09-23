package com.example.ckgl.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.ckgl.common.Result;
import com.example.ckgl.dto.InboundRequest;
import com.example.ckgl.dto.OutboundRequest;
import com.example.ckgl.dto.RecordQuery;
import com.example.ckgl.dto.RecordVO;
import com.example.ckgl.entity.StockRecord;
import com.example.ckgl.service.StockRecordService;
import com.example.ckgl.service.StockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/records")
public class StockRecordController {

    @Autowired
    private StockService stockService;
    @Autowired
    private StockRecordService stockRecordService;

    @PostMapping("/inbound")
    public Result<StockRecord> inbound(@Valid @RequestBody InboundRequest req) {
        return Result.ok(stockService.inbound(req));
    }

    @PostMapping("/outbound")
    public Result<StockRecord> outbound(@Valid @RequestBody OutboundRequest req) {
        return Result.ok(stockService.outbound(req));
    }

    @GetMapping
    public Result<IPage<RecordVO>> list(RecordQuery query) {
        return Result.ok(stockRecordService.page(query));
    }
}
