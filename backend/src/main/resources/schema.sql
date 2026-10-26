CREATE TABLE IF NOT EXISTS part (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(64)  NOT NULL COMMENT '零件型号(唯一编码)',
  name       VARCHAR(128) NOT NULL COMMENT '零件名称',
  unit       VARCHAR(16)  DEFAULT '' COMMENT '单位(个/件/米等)',
  category   VARCHAR(64)  DEFAULT '' COMMENT '分类',
  remark     VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_part_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='零件';

CREATE TABLE IF NOT EXISTS product (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(64)  NOT NULL COMMENT '产品编码',
  name       VARCHAR(128) NOT NULL COMMENT '产品名称',
  remark     VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品';

CREATE TABLE IF NOT EXISTS product_part (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  part_id    BIGINT NOT NULL,
  quantity   INT    NOT NULL DEFAULT 1 COMMENT '该产品使用该零件的数量',
  UNIQUE KEY uk_product_part (product_id, part_id),
  KEY idx_pp_part (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品零件关联';

CREATE TABLE IF NOT EXISTS warehouse (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(64)  NOT NULL COMMENT '仓库编码',
  name       VARCHAR(128) NOT NULL COMMENT '仓库名称',
  remark     VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_warehouse_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库';

CREATE TABLE IF NOT EXISTS location (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  warehouse_id BIGINT      NOT NULL,
  code         VARCHAR(64) NOT NULL COMMENT '货位编号(仓库内唯一)',
  remark       VARCHAR(255) DEFAULT '',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_location (warehouse_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货位';

CREATE TABLE IF NOT EXISTS stock (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  location_id BIGINT NOT NULL,
  part_id     BIGINT NOT NULL,
  quantity    INT    NOT NULL DEFAULT 0,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_stock (location_id, part_id),
  KEY idx_stock_part (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货位零件库存';

CREATE TABLE IF NOT EXISTS stock_record (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  type         VARCHAR(8)   NOT NULL COMMENT 'IN=入库 OUT=出库',
  warehouse_id BIGINT       NOT NULL COMMENT '冗余仓库id，便于按仓库筛选',
  location_id  BIGINT       NOT NULL,
  part_id      BIGINT       NOT NULL,
  quantity     INT          NOT NULL COMMENT '本次出入库数量(正数)',
  remark       VARCHAR(255) DEFAULT '',
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_record_type_time (type, created_at),
  KEY idx_record_location (location_id),
  KEY idx_record_part (part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出入库流水';

CREATE TABLE IF NOT EXISTS production_plan (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  plan_date  DATE         NOT NULL COMMENT '计划生产日期',
  line       VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '产线(1线/2线/3线...)',
  product_id BIGINT       NOT NULL COMMENT '产品id',
  quantity   INT          NOT NULL COMMENT '计划数量',
  completed  INT          NOT NULL DEFAULT 0 COMMENT '已完成数量(实时更新，避免预警误报)',
  remark     VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_plan_date (plan_date),
  KEY idx_plan_product (product_id),
  KEY idx_plan_line_date (line, plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划';

CREATE TABLE IF NOT EXISTS part_production_in (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  part_id    BIGINT NOT NULL COMMENT '零件id',
  in_date    DATE   NOT NULL COMMENT '生产入库日期',
  quantity   INT    NOT NULL COMMENT '入库数量',
  remark     VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_pin_part_date (part_id, in_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='源头零件生产入库台账';

CREATE TABLE IF NOT EXISTS part_production_out (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  part_id    BIGINT NOT NULL COMMENT '零件id',
  out_date   DATE   NOT NULL COMMENT '产线消耗日期',
  quantity   INT    NOT NULL COMMENT '消耗数量',
  remark     VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_pout_part_date (part_id, out_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='尾端产线消耗台账';

CREATE TABLE IF NOT EXISTS part_month_settle (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  part_id     BIGINT      NOT NULL COMMENT '零件id',
  month_key   VARCHAR(7)  NOT NULL COMMENT '结算后的月份(YYYY-MM)，其期初=上月期末',
  opening_qty INT         NOT NULL DEFAULT 0 COMMENT '结转期初库存',
  settled_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_settle_part_month (part_id, month_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产台账月度结算';
