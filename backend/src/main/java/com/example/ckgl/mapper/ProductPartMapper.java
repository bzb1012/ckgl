package com.example.ckgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.ckgl.dto.ProductPartDetail;
import com.example.ckgl.dto.ProductPartExportVO;
import com.example.ckgl.entity.ProductPart;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProductPartMapper extends BaseMapper<ProductPart> {

    @Select("SELECT pp.part_id AS partId, p.code AS partCode, p.name AS partName, p.unit AS unit, p.category AS category, pp.quantity " +
            "FROM product_part pp JOIN part p ON p.id = pp.part_id " +
            "WHERE pp.product_id = #{productId} ORDER BY p.code")
    List<ProductPartDetail> selectByProductId(@Param("productId") Long productId);

    /** 全部产品的零件用量（导出用） */
    @Select("SELECT pp.product_id AS productId, p.code AS partCode, p.name AS partName, p.category AS category, p.unit AS unit, pp.quantity " +
            "FROM product_part pp JOIN part p ON p.id = pp.part_id ORDER BY pp.product_id, p.code")
    List<ProductPartExportVO> selectAllWithPart();
}
