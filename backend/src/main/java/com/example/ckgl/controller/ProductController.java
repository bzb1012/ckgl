package com.example.ckgl.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ckgl.common.Result;
import com.example.ckgl.dto.ProductDTO;
import com.example.ckgl.dto.ProductDetailVO;
import com.example.ckgl.entity.Product;
import com.example.ckgl.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public Result<Page<Product>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        return Result.ok(productService.page(keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @PostMapping
    public Result<Product> create(@Valid @RequestBody ProductDTO dto) {
        return Result.ok(productService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        productService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
