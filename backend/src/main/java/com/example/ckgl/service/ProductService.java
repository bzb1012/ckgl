package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ckgl.common.BusinessException;
import com.example.ckgl.dto.ProductDTO;
import com.example.ckgl.dto.ProductDetailVO;
import com.example.ckgl.dto.ProductPartDetail;
import com.example.ckgl.dto.ProductPartItem;
import com.example.ckgl.entity.Product;
import com.example.ckgl.entity.ProductPart;
import com.example.ckgl.mapper.PartMapper;
import com.example.ckgl.mapper.ProductPartMapper;
import com.example.ckgl.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    @Autowired
    private ProductPartMapper productPartMapper;
    @Autowired
    private PartMapper partMapper;

    public Page<Product> page(String keyword, long page, long size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Product::getCode, keyword).or().like(Product::getName, keyword));
        }
        wrapper.orderByDesc(Product::getId);
        Page<Product> result = this.page(new Page<>(page, size), wrapper);
        fillPartCount(result);
        return result;
    }

    private void fillPartCount(Page<Product> result) {
        List<Product> records = result.getRecords();
        if (records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(Product::getId).toList();
        QueryWrapper<ProductPart> qw = new QueryWrapper<ProductPart>()
                .select("product_id AS productId", "COUNT(*) AS cnt")
                .in("product_id", ids)
                .groupBy("product_id");
        Map<Long, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : productPartMapper.selectMaps(qw)) {
            countMap.put(((Number) row.get("productId")).longValue(), ((Number) row.get("cnt")).longValue());
        }
        records.forEach(p -> p.setPartCount(countMap.getOrDefault(p.getId(), 0L).intValue()));
    }

    public ProductDetailVO detail(Long id) {
        Product product = this.getById(id);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setCode(product.getCode());
        vo.setName(product.getName());
        vo.setRemark(product.getRemark());
        vo.setParts(productPartMapper.selectByProductId(id));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Product create(ProductDTO dto) {
        Product product = new Product();
        product.setCode(dto.getCode().trim());
        product.setName(dto.getName().trim());
        product.setRemark(dto.getRemark());
        this.save(product);
        replaceParts(product.getId(), dto.getParts());
        return product;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductDTO dto) {
        Product product = this.getById(id);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        product.setCode(dto.getCode().trim());
        product.setName(dto.getName().trim());
        product.setRemark(dto.getRemark());
        this.updateById(product);
        replaceParts(id, dto.getParts());
    }

    /** 全量替换产品的零件绑定 */
    private void replaceParts(Long productId, List<ProductPartItem> parts) {
        productPartMapper.delete(new LambdaQueryWrapper<ProductPart>().eq(ProductPart::getProductId, productId));
        if (parts == null || parts.isEmpty()) {
            return;
        }
        for (ProductPartItem item : parts) {
            if (partMapper.selectById(item.getPartId()) == null) {
                throw new BusinessException("零件不存在：id=" + item.getPartId());
            }
            ProductPart pp = new ProductPart();
            pp.setProductId(productId);
            pp.setPartId(item.getPartId());
            pp.setQuantity(item.getQuantity());
            productPartMapper.insert(pp);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("产品不存在");
        }
        productPartMapper.delete(new LambdaQueryWrapper<ProductPart>().eq(ProductPart::getProductId, id));
        this.removeById(id);
    }
}
