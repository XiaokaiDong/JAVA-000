--mysql建表语句
CREATE TABLE `tb_orders` (
	`id` INT(13) NOT NULL AUTO_INCREMENT COMMENT 'id',
	`order_id` BIGINT(64)  NOT NULL COMMENT '订单ID，有ID生成服务生成',
	`order_version` INT(13) NOT NULL COMMENT '订单版本号，防止ABA等更新问题',
	`create_time` BIGINT(64) NOT NULL  COMMENT '创建时间，为了时钟同步，不使用MYSQL时间戳',
	`modified_time` BIGINT(64) NOT NULL  COMMENT '修改时间，为了时钟同步，不使用MYSQL时间戳',
	`skuid` VARCHAR(255) NOT NULL COMMENT 'sku ID' COLLATE 'utf8mb4_bin',
	`sku_version` VARCHAR(255) NOT NULL COMMENT 'sku版本号' COLLATE 'utf8mb4_bin',
	`from_id` VARCHAR(128) NOT NULL COMMENT '卖家ID' COLLATE 'utf8mb4_bin',
	`to_id` VARCHAR(128) NOT NULL COMMENT '买家ID' COLLATE 'utf8mb4_bin',
	`final_price` INT(13) NOT NULL COMMENT '最终价格',
	`status` VARCHAR(20) NOT NULL COMMENT '订单状态' COLLATE 'utf8mb4_bin',
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `idx_order` (`order_id`) USING BTREE
)
COMMENT='订单主表'
COLLATE='utf8mb4_bin'
ENGINE=InnoDB
;


CREATE TABLE `tb_commodities` (
	`id` INT(13) NOT NULL AUTO_INCREMENT COMMENT '商品id',
	`name` VARCHAR(255) NOT NULL COMMENT '商品名称' COLLATE 'utf8mb4_bin',
	`skuid` VARCHAR(255) NOT NULL COMMENT '商品编号' COLLATE 'utf8mb4_bin',
	`version` VARCHAR(255) NOT NULL COMMENT '商品版本号，调价时会重新生成' COLLATE 'utf8mb4_bin',
	`nominally_price` INT(13) NOT NULL COMMENT '商品标价，以分为单位',
	`create_time` BIGINT(64) NOT NULL COMMENT '创建时间戳。没有修改时间，修改价格时，就需要重新生成一个版本号',
	`desc` VARCHAR(2048) NULL DEFAULT NULL COMMENT '商品描述' COLLATE 'utf8mb4_bin',
	`major_type` VARCHAR(128) NOT NULL COMMENT '商品大类，可用于分库分表' COLLATE 'utf8mb4_bin',
	`minor_type` VARCHAR(128) NOT NULL COMMENT '商品小类，可用于分库分表' COLLATE 'utf8mb4_bin',
	`parameters` VARCHAR(128) NOT NULL COMMENT '商品参数' COLLATE 'utf8mb4_bin',
	`merchant_id` INT(13) NOT NULL COMMENT '所属商户ID',
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `uk_sku_ver` (`skuid`, `version`) USING BTREE,
	INDEX `idx_create_time` (`create_time`) USING BTREE
)
COMMENT='商品表。这里只保存商品的基本信息等结构化信息；至于商品特征等非结构化数据，放入面向的文档数据库。\r\n图片和视频放入对象存储。\r\n此表不允许更新与删除，只允许新增，新增时更新版本号。'
COLLATE='utf8mb4_bin'
ENGINE=InnoDB
;

CREATE TABLE `tb_user` (
	`id` INT(13) NOT NULL AUTO_INCREMENT COMMENT 'ID',
	`user_id` VARCHAR(128) NOT NULL COMMENT '用户ID，用户的唯一标识，具有全局唯一性' COLLATE 'utf8mb4_general_ci',
	`name` VARCHAR(255) NOT NULL COMMENT '用户名称' COLLATE 'utf8mb4_bin',
	`passwd` VARCHAR(255) NOT NULL COMMENT '密码' COLLATE 'utf8mb4_general_ci',
	`mobile_phone` VARCHAR(20) NOT NULL COMMENT '用户手机号' COLLATE 'utf8mb4_general_ci',
	`certified` INT(1) NOT NULL COMMENT '实名认证类型',
	`score` INT(11) NOT NULL DEFAULT '0' COMMENT '积分',
	`type` INT(4) NOT NULL COMMENT '用户类型：1，普通用户；2，商户；3，即是用户又是商户',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `id_userid_name` (`user_id`, `name`)
)
COMMENT='用户表。\r\n此表只保存用户基本信息。\r\n至于用户头像独存储。\r\n用户权限单独存储，符合关系数据库的范式。\r\n登陆历史、浏览历史需要单独保存。'
COLLATE='utf8mb4_general_ci'
ENGINE=InnoDB
;


--下面是ORACLE的订单表建表语句
-- Create table
create table ORDERS
(
  pid           NUMBER not null,
  order_id      NUMBER not null,
  order_version NUMBER not null,
  create_time   NUMBER not null,
  modified_time NUMBER not null,
  skuid         VARCHAR2(255) not null,
  sku_version   VARCHAR2(255) not null,
  from_id       VARCHAR2(128) not null,
  to_id         VARCHAR2(128) not null,
  final_price   NUMBER not null,
  status        VARCHAR2(20) not null
)
tablespace CSPSDAT
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64
    next 1
    minextents 1
    maxextents unlimited
  );
-- Create/Recreate indexes
create unique index IND_ORDER_ID on ORDERS (ORDER_ID)
  tablespace CSPSDAT
  pctfree 10
  initrans 2
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
-- Create/Recreate primary, unique and foreign key constraints
alter table ORDERS
  add constraint PK_ORDER primary key (PID)
  using index
  tablespace CSPSDAT
  pctfree 10
  initrans 2
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );

