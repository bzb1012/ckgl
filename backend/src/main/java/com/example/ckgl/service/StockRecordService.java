package com.example.ckgl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ckgl.dto.RecordQuery;
import com.example.ckgl.dto.RecordVO;
import com.example.ckgl.entity.StockRecord;
import com.example.ckgl.mapper.StockRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class StockRecordService extends ServiceImpl<StockRecordMapper, StockRecord> {

    public IPage<RecordVO> page(RecordQuery q) {
        // 前端传 yyyy-MM-dd 时补全时分秒，保证 endTime 包含当天
        if (q.getStartTime() != null && q.getStartTime().length() == 10) {
            q.setStartTime(q.getStartTime() + " 00:00:00");
        }
        if (q.getEndTime() != null && q.getEndTime().length() == 10) {
            q.setEndTime(q.getEndTime() + " 23:59:59");
        }
        return baseMapper.selectRecordPage(new Page<>(q.getPage(), q.getSize()), q);
    }
}
