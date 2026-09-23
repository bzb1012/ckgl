package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.LocationDTO;
import com.example.ckgl.dto.WarehouseDTO;
import com.example.ckgl.entity.Location;
import com.example.ckgl.entity.Stock;
import com.example.ckgl.entity.StockRecord;
import com.example.ckgl.entity.Warehouse;
import com.example.ckgl.mapper.LocationMapper;
import com.example.ckgl.mapper.StockMapper;
import com.example.ckgl.mapper.StockRecordMapper;
import com.example.ckgl.mapper.WarehouseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseService extends ServiceImpl<WarehouseMapper, Warehouse> {

    @Autowired
    private LocationMapper locationMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private StockRecordMapper stockRecordMapper;

    public List<Warehouse> listWithLocationCount() {
        List<Warehouse> list = this.list(new LambdaQueryWrapper<Warehouse>().orderByAsc(Warehouse::getId));
        if (list.isEmpty()) {
            return list;
        }
        QueryWrapper<Location> qw = new QueryWrapper<Location>()
                .select("warehouse_id AS warehouseId", "COUNT(*) AS cnt")
                .groupBy("warehouse_id");
        Map<Long, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : locationMapper.selectMaps(qw)) {
            countMap.put(((Number) row.get("warehouseId")).longValue(), ((Number) row.get("cnt")).longValue());
        }
        list.forEach(w -> w.setLocationCount(countMap.getOrDefault(w.getId(), 0L).intValue()));
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public Warehouse create(WarehouseDTO dto) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(dto.getCode().trim());
        warehouse.setName(dto.getName().trim());
        warehouse.setRemark(dto.getRemark());
        this.save(warehouse);
        return warehouse;
    }

    public void update(Long id, WarehouseDTO dto) {
        Warehouse warehouse = this.getById(id);
        if (warehouse == null) {
            throw new BusinessException("仓库不存在");
        }
        warehouse.setCode(dto.getCode().trim());
        warehouse.setName(dto.getName().trim());
        warehouse.setRemark(dto.getRemark());
        this.updateById(warehouse);
    }

    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("仓库不存在");
        }
        if (locationMapper.selectCount(new LambdaQueryWrapper<Location>().eq(Location::getWarehouseId, id)) > 0) {
            throw new BusinessException("该仓库下存在货位，无法删除");
        }
        this.removeById(id);
    }

    public List<Location> listLocations(Long warehouseId) {
        return locationMapper.selectList(new LambdaQueryWrapper<Location>()
                .eq(Location::getWarehouseId, warehouseId)
                .orderByAsc(Location::getCode));
    }

    public Location addLocation(Long warehouseId, LocationDTO dto) {
        if (this.getById(warehouseId) == null) {
            throw new BusinessException("仓库不存在");
        }
        Location location = new Location();
        location.setWarehouseId(warehouseId);
        location.setCode(dto.getCode().trim());
        location.setRemark(dto.getRemark());
        locationMapper.insert(location);
        return location;
    }

    public void updateLocation(Long id, LocationDTO dto) {
        Location location = locationMapper.selectById(id);
        if (location == null) {
            throw new BusinessException("货位不存在");
        }
        location.setCode(dto.getCode().trim());
        location.setRemark(dto.getRemark());
        locationMapper.updateById(location);
    }

    public void deleteLocation(Long id) {
        Location location = locationMapper.selectById(id);
        if (location == null) {
            throw new BusinessException("货位不存在");
        }
        if (stockMapper.selectCount(new LambdaQueryWrapper<Stock>().eq(Stock::getLocationId, id)) > 0) {
            throw new BusinessException("该货位仍有库存，无法删除");
        }
        if (stockRecordMapper.selectCount(new LambdaQueryWrapper<StockRecord>().eq(StockRecord::getLocationId, id)) > 0) {
            throw new BusinessException("该货位存在出入库记录，无法删除");
        }
        locationMapper.deleteById(id);
    }
}
