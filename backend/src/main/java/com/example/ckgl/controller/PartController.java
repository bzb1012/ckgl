package com.example.ckgl.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ckgl.common.Result;
import com.example.ckgl.dto.PartDTO;
import com.example.ckgl.entity.Part;
import com.example.ckgl.service.PartService;
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

import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    @Autowired
    private PartService partService;

    @GetMapping
    public Result<Page<Part>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "10") long size) {
        return Result.ok(partService.page(keyword, category, page, size));
    }

    /** 已使用的零件分类列表（去重） */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(partService.categories());
    }

    @GetMapping("/{id}")
    public Result<Part> detail(@PathVariable Long id) {
        return Result.ok(partService.getById(id));
    }

    @PostMapping
    public Result<Part> create(@Valid @RequestBody PartDTO dto) {
        return Result.ok(partService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PartDTO dto) {
        partService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        partService.delete(id);
        return Result.ok();
    }
}
