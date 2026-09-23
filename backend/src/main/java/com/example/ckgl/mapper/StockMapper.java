package com.example.ckgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ckgl.dto.StockQuery;
import com.example.ckgl.dto.StockVO;
import com.example.ckgl.entity.Stock;
import org.apache.ibatis.annotations.Param;

public interface StockMapper extends BaseMapper<Stock> {

    IPage<StockVO> selectStockPage(Page<StockVO> page, @Param("q") StockQuery q);
}
