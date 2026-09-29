package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.ProductPartDetail;
import com.example.ckgl.dto.StockItemVO;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 库存盘点：按货位直接修改数量、清空、增删物料 */
@Service
public class StockTakeService {

    @Autowired
    private LocationMapper locationMapper;
    @Autowired
    private PartMapper partMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private StockRecordMapper stockRecordMapper;

    /** 货位内全部物料（含 0 库存），按零件型号排序 */
    public List<StockItemVO> locationItems(Long locationId) {
        requireLocation(locationId);
        Map<Long, Stock> stockMap = stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                        .eq(Stock::getLocationId, locationId)).stream()
                .collect(Collectors.toMap(Stock::getPartId, Function.identity()));
        List<StockItemVO> items = new ArrayList<>();
        for (Stock s : stockMap.values()) {
            Part p = partMapper.selectById(s.getPartId());
            if (p == null) {
                continue;
            }
            items.add(toVO(p, s.getQuantity()));
        }
        items.sort(Comparator.comparing(StockItemVO::getPartCode));
        return items;
    }

    /** 盘点直接修正数量（含改为 0），差额记一条盘点出入库流水 */
    @Transactional(rollbackFor = Exception.class)
    public void setQuantity(Long locationId, Long partId, int quantity) {
        requireLocation(locationId);
        requirePart(partId);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getLocationId, locationId)
                .eq(Stock::getPartId, partId));
        if (stock == null) {
            throw new BusinessException("该货位没有此零件，请先添加物料");
        }
        int old = stock.getQuantity();
        if (old == quantity) {
            return;
        }
        stockMapper.update(null, new UpdateWrapper<Stock>()
                .setSql("quantity = {0}", quantity)
                .eq("location_id", locationId)
                .eq("part_id", partId));
        // 盘点差额留痕：增加记 IN、减少记 OUT，备注标明盘点
        saveRecord(quantity > old ? "IN" : "OUT", locationId, partId, Math.abs(quantity - old), "盘点修正");
    }

    /** 一键清空：全部置 0 并逐条留痕，返回清空的物料数 */
    @Transactional(rollbackFor = Exception.class)
    public int clearLocation(Long locationId) {
        requireLocation(locationId);
        int count = 0;
        for (Stock s : stockMapper.selectList(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getLocationId, locationId))) {
            if (s.getQuantity() > 0) {
                stockMapper.update(null, new UpdateWrapper<Stock>()
                        .setSql("quantity = 0")
                        .eq("id", s.getId()));
                saveRecord("OUT", locationId, s.getPartId(), s.getQuantity(), "盘点清空");
            }
            count++;
        }
        return count;
    }

    /** 增加物料：已有则累加数量，否则新建；数量留痕 */
    @Transactional(rollbackFor = Exception.class)
    public void addPart(Long locationId, Long partId, int quantity) {
        requireLocation(locationId);
        requirePart(partId);
        int rows = stockMapper.update(null, new UpdateWrapper<Stock>()
                .setSql("quantity = quantity + {0}", quantity)
                .eq("location_id", locationId)
                .eq("part_id", partId));
        if (rows == 0) {
            Stock stock = new Stock();
            stock.setLocationId(locationId);
            stock.setPartId(partId);
            stock.setQuantity(quantity);
            try {
                stockMapper.insert(stock);
            } catch (DuplicateKeyException e) {
                // 并发首次添加撞唯一索引：回退为累加
                stockMapper.update(null, new UpdateWrapper<Stock>()
                        .setSql("quantity = quantity + {0}", quantity)
                        .eq("location_id", locationId)
                        .eq("part_id", partId));
            }
        }
        saveRecord("IN", locationId, partId, quantity, "盘点添加");
    }

    /** 删除物料：整行移除（不流水，行已消失），数量不为 0 时先记一条 OUT */
    @Transactional(rollbackFor = Exception.class)
    public void removePart(Long locationId, Long partId) {
        requireLocation(locationId);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getLocationId, locationId)
                .eq(Stock::getPartId, partId));
        if (stock == null) {
            throw new BusinessException("该货位没有此零件");
        }
        if (stock.getQuantity() > 0) {
            saveRecord("OUT", locationId, partId, stock.getQuantity(), "盘点删除");
        }
        stockMapper.deleteById(stock.getId());
    }

    private StockItemVO toVO(Part p, int quantity) {
        StockItemVO vo = new StockItemVO();
        vo.setPartId(p.getId());
        vo.setPartCode(p.getCode());
        vo.setPartName(p.getName());
        vo.setUnit(p.getUnit());
        vo.setCategory(p.getCategory());
        vo.setQuantity(quantity);
        return vo;
    }

    private void saveRecord(String type, Long locationId, Long partId, int quantity, String remark) {
        Location location = locationMapper.selectById(locationId);
        StockRecord record = new StockRecord();
        record.setType(type);
        record.setWarehouseId(location.getWarehouseId());
        record.setLocationId(locationId);
        record.setPartId(partId);
        record.setQuantity(quantity);
        record.setRemark(remark);
        stockRecordMapper.insert(record);
    }

    private void requireLocation(Long id) {
        if (locationMapper.selectById(id) == null) {
            throw new BusinessException("货位不存在");
        }
    }

    private void requirePart(Long id) {
        if (partMapper.selectById(id) == null) {
            throw new BusinessException("零件不存在");
        }
    }
}
