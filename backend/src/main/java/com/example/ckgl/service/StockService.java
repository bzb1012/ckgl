package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.InboundRequest;
import com.example.ckgl.dto.OutboundRequest;
import com.example.ckgl.dto.StockQuery;
import com.example.ckgl.dto.StockVO;
import com.example.ckgl.entity.Location;
import com.example.ckgl.entity.Part;
import com.example.ckgl.entity.Stock;
import com.example.ckgl.entity.StockRecord;
import com.example.ckgl.mapper.LocationMapper;
import com.example.ckgl.mapper.PartMapper;
import com.example.ckgl.mapper.StockMapper;
import com.example.ckgl.mapper.StockRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    @Autowired
    private LocationMapper locationMapper;
    @Autowired
    private PartMapper partMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private StockRecordMapper stockRecordMapper;

    public IPage<StockVO> pageStocks(StockQuery q) {
        return stockMapper.selectStockPage(new Page<>(q.getPage(), q.getSize()), q);
    }

    @Transactional(rollbackFor = Exception.class)
    public StockRecord inbound(InboundRequest req) {
        Location location = requireLocation(req.getLocationId());
        requirePart(req.getPartId());
        int rows = stockMapper.update(null, new UpdateWrapper<Stock>()
                .setSql("quantity = quantity + {0}", req.getQuantity())
                .eq("location_id", req.getLocationId())
                .eq("part_id", req.getPartId()));
        if (rows == 0) {
            Stock stock = new Stock();
            stock.setLocationId(req.getLocationId());
            stock.setPartId(req.getPartId());
            stock.setQuantity(req.getQuantity());
            try {
                stockMapper.insert(stock);
            } catch (DuplicateKeyException e) {
                // 并发下两条首次入库同时插入撞唯一索引：回退为累加
                stockMapper.update(null, new UpdateWrapper<Stock>()
                        .setSql("quantity = quantity + {0}", req.getQuantity())
                        .eq("location_id", req.getLocationId())
                        .eq("part_id", req.getPartId()));
            }
        }
        return saveRecord("IN", location, req.getPartId(), req.getQuantity(), req.getRemark());
    }

    @Transactional(rollbackFor = Exception.class)
    public StockRecord outbound(OutboundRequest req) {
        Location location = requireLocation(req.getLocationId());
        requirePart(req.getPartId());
        // 带条件的原子扣减：库存充足才会更新
        int rows = stockMapper.update(null, new UpdateWrapper<Stock>()
                .setSql("quantity = quantity - {0}", req.getQuantity())
                .eq("location_id", req.getLocationId())
                .eq("part_id", req.getPartId())
                .ge("quantity", req.getQuantity()));
        if (rows == 0) {
            Stock current = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                    .eq(Stock::getLocationId, req.getLocationId())
                    .eq(Stock::getPartId, req.getPartId()));
            if (current == null) {
                throw new BusinessException("该货位没有此零件的库存");
            }
            throw new BusinessException("库存不足，当前库存 " + current.getQuantity());
        }
        return saveRecord("OUT", location, req.getPartId(), req.getQuantity(), req.getRemark());
    }

    private StockRecord saveRecord(String type, Location location, Long partId, Integer quantity, String remark) {
        StockRecord record = new StockRecord();
        record.setType(type);
        record.setWarehouseId(location.getWarehouseId());
        record.setLocationId(location.getId());
        record.setPartId(partId);
        record.setQuantity(quantity);
        record.setRemark(remark);
        stockRecordMapper.insert(record);
        return record;
    }

    private Location requireLocation(Long id) {
        Location location = locationMapper.selectById(id);
        if (location == null) {
            throw new BusinessException("货位不存在");
        }
        return location;
    }

    private void requirePart(Long id) {
        if (partMapper.selectById(id) == null) {
            throw new BusinessException("零件不存在");
        }
    }
}
