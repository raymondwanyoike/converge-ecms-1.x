CREATE TABLE `log_entry` (
    `id` BIGINT NOT NULL AUTO_INCREMENT ,
    `description` TEXT DEFAULT '',
    `severity` VARCHAR(255) DEFAULT '',
    `log_date` datetime DEFAULT null,
    `actor_id` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FK_log_entry_actor` (`actor_id`)
);

CREATE TABLE `log_subject` (
    `id` BIGINT NOT NULL AUTO_INCREMENT ,
    `entity` VARCHAR(255) DEFAULT '',
    `entity_id` VARCHAR(255) DEFAULT '',
    `link` VARCHAR(255) DEFAULT '',
    `log_entry_id` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE INDEX `idx_severity` ON `log_entry` (`severity`);
CREATE INDEX `idx_entity_and_id` ON `log_subject` (`entity`, `entity_id`);
CREATE INDEX `idx_entity` ON `log_subject` (`entity`);

ALTER TABLE newswire_service ADD COLUMN `processing` TINYINT(1) DEFAULT '0';
ALTER TABLE media_item ADD COLUMN `held` TINYINT(1) DEFAULT '0';