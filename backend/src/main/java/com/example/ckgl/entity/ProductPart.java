package com.example.ckgl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_part")
public class ProductPart {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long partId;
    /** 该产品使用该零件的数量 */
    private Integer quantity;
}
