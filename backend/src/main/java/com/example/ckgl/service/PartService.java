package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.PartDTO;
import com.example.ckgl.entity.Part;
import com.example.ckgl.entity.ProductPart;
import com.example.ckgl.entity.Stock;
import com.example.ckgl.entity.StockRecord;
import com.example.ckgl.mapper.PartMapper;
import com.example.ckgl.mapper.ProductPartMapper;
import com.example.ckgl.mapper.StockMapper;
import com.example.ckgl.mapper.StockRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PartService extends ServiceImpl<PartMapper, Part> {

    @Autowired
    private ProductPartMapper productPartMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private StockRecordMapper stockRecordMapper;

    public Page<Part> page(String keyword, String category, long page, long size) {
        LambdaQueryWrapper<Part> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Part::getCode, keyword).or().like(Part::getName, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Part::getCategory, category);
        }
        wrapper.orderByDesc(Part::getId);
        return this.page(new Page<>(page, size), wrapper);
    }

    /** 所有已使用的分类（去重，供下拉选择） */
    public List<String> categories() {
        return this.list(new LambdaQueryWrapper<Part>()
                        .select(Part::getCategory)
                        .ne(Part::getCategory, "")
                        .groupBy(Part::getCategory))
                .stream().map(Part::getCategory).toList();
    }

    public Part create(PartDTO dto) {
        Part part = new Part();
        part.setCode(dto.getCode().trim());
        part.setName(dto.getName().trim());
        part.setUnit(dto.getUnit());
        part.setCategory(dto.getCategory());
        part.setRemark(dto.getRemark());
        this.save(part);
        return part;
    }

    public void update(Long id, PartDTO dto) {
        Part part = this.getById(id);
        if (part == null) {
            throw new BusinessException("零件不存在");
        }
        part.setCode(dto.getCode().trim());
        part.setName(dto.getName().trim());
        part.setUnit(dto.getUnit());
        part.setCategory(dto.getCategory());
        part.setRemark(dto.getRemark());
        this.updateById(part);
    }

    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("零件不存在");
        }
        if (productPartMapper.selectCount(new LambdaQueryWrapper<ProductPart>().eq(ProductPart::getPartId, id)) > 0) {
            throw new BusinessException("该零件已被产品引用，无法删除");
        }
        if (stockMapper.selectCount(new LambdaQueryWrapper<Stock>().eq(Stock::getPartId, id)) > 0) {
            throw new BusinessException("该零件仍有库存，无法删除");
        }
        if (stockRecordMapper.selectCount(new LambdaQueryWrapper<StockRecord>().eq(StockRecord::getPartId, id)) > 0) {
            throw new BusinessException("该零件存在出入库记录，无法删除");
        }
        this.removeById(id);
    }
}
