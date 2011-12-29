CREATE TABLE `log_entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT ,
    `description` TEXT DEFAULT '',
    `severity` VARCHAR(255) DEFAULT '',
    `origin` VARCHAR(255) DEFAULT '',
    `origin_id` VARCHAR(255) DEFAULT '',
    `log_date` datetime DEFAULT null,
    `actor_id` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FK_log_entry_actor` (`actor_id`)
);

CREATE INDEX `idx_severity` ON `log_entry` (`severity`);
CREATE INDEX `idx_origin` ON `log_entry` (`origin`);
CREATE INDEX `idx_origin_id` ON `log_entry` (`origin_id`);
