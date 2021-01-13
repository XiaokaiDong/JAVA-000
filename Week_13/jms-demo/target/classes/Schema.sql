--mysql建表语句
CREATE TABLE `tb_messages` (
	`id` INT(13) NOT NULL AUTO_INCREMENT COMMENT 'id',
	`destination` VARCHAR(255) NOT NULL COMMENT '消息类别' COLLATE 'utf8mb4_bin',
	`content` VARCHAR(2000) NOT NULL COMMENT '消息内容' COLLATE 'utf8mb4_bin',
	`processed` INT(1) NOT NULL COMMENT '是否被处理: 0，未处理;1，已处理;2,处理中' COLLATE 'utf8mb4_bin',
	`processor_id` VARCHAR(255) NOT NULL COMMENT '处理者ID' COLLATE 'utf8mb4_bin',
	`create_time` BIGINT(64) NOT NULL  COMMENT '创建时间，为了时钟同步，不使用MYSQL时间戳',
	`modified_time` BIGINT(64) COMMENT '修改时间，为了时钟同步，不使用MYSQL时间戳',
	PRIMARY KEY (`id`) USING BTREE,
	UNIQUE INDEX `idx_order` (`order_id`) USING BTREE
)
COMMENT='消息表'
COLLATE='utf8mb4_bin'
ENGINE=InnoDB
;