package com.example.ckgl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ckgl.dto.RecordQuery;
import com.example.ckgl.dto.RecordVO;
import com.example.ckgl.entity.StockRecord;
import org.apache.ibatis.annotations.Param;

public interface StockRecordMapper extends BaseMapper<StockRecord> {

    IPage<RecordVO> selectRecordPage(Page<RecordVO> page, @Param("q") RecordQuery q);
}
