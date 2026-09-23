package com.example.ckgl.controller;

import com.example.ckgl.common.Result;
import com.example.ckgl.dto.LocationDTO;
import com.example.ckgl.dto.WarehouseDTO;
import com.example.ckgl.entity.Location;
import com.example.ckgl.entity.Warehouse;
import com.example.ckgl.service.WarehouseService;
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

@RestController
@RequestMapping("/api")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @GetMapping("/warehouses")
    public Result<List<Warehouse>> list() {
        return Result.ok(warehouseService.listWithLocationCount());
    }

    @PostMapping("/warehouses")
    public Result<Warehouse> create(@Valid @RequestBody WarehouseDTO dto) {
        return Result.ok(warehouseService.create(dto));
    }

    @PutMapping("/warehouses/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WarehouseDTO dto) {
        warehouseService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/warehouses/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok();
    }

    @GetMapping("/warehouses/{id}/locations")
    public Result<List<Location>> listLocations(@PathVariable Long id) {
        return Result.ok(warehouseService.listLocations(id));
    }

    @PostMapping("/warehouses/{id}/locations")
    public Result<Location> addLocation(@PathVariable Long id, @Valid @RequestBody LocationDTO dto) {
        return Result.ok(warehouseService.addLocation(id, dto));
    }

    @PutMapping("/locations/{id}")
    public Result<Void> updateLocation(@PathVariable Long id, @Valid @RequestBody LocationDTO dto) {
        warehouseService.updateLocation(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/locations/{id}")
    public Result<Void> deleteLocation(@PathVariable Long id) {
        warehouseService.deleteLocation(id);
        return Result.ok();
    }
}
