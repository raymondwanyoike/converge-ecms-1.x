ALTER TABLE rendition DROP COLUMN opt_lock;
ALTER TABLE rendition ADD COLUMN label varchar(255);
ALTER TABLE media_item DROP COLUMN parent_id;

RENAME TABLE media_repository TO catalogue;
ALTER TABLE catalogue ADD COLUMN preview_rendition BIGINT;
ALTER TABLE catalogue ADD COLUMN original_rendition BIGINT;

ALTER TABLE `media_item` CHANGE COLUMN `media_repository_id` `catalogue_id` BIGINT(20) NULL DEFAULT NULL, 
DROP INDEX `FK_media_item_media_repository_id`, 
ADD INDEX `FK_media_item_media_repository_id` (`catalogue_id` ASC) ;

INSERT INTO rendition (name, label, description) VALUES ('highRes', 'High Resolution', 'Original rendition');

CREATE  TABLE `media_item_rendition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT ,
  `rendition_id` BIGINT NULL ,
  `filename` TEXT DEFAULT '',
  `path` TEXT DEFAULT '',
  `content_type` VARCHAR(255) DEFAULT '' ,
  `file_size` BIGINT DEFAULT 0,
  `width` INT NULL ,
  `height` INT NULL ,
  `colourSpace` VARCHAR(255) DEFAULT '' ,
  `resolution` INT NULL ,
  `audio_bitrate` INT NULL ,
  `audio_channels` VARCHAR(255) DEFAULT '',
  `audio_codec` VARCHAR(255) DEFAULT '',
  `audio_sample_size` INT NULL ,
  `audio_sample_rate` INT NULL ,
  `audio_variable_bitrate` TINYINT(1) DEFAULT '0',
  `duration` INT NULL,
  `video_codec` VARCHAR(255) DEFAULT '',
  `video_average_bit_rate` INT NULL,
  `video_variable_bit_rate` TINYINT(1) DEFAULT '0',
  `video_frame_rate` INT NULL,
  `video_scan_technique` VARCHAR(255) DEFAULT '',
  `video_aspect_ratio` VARCHAR(255) DEFAULT '',
  `video_sampling_method` VARCHAR(255) DEFAULT '',
  `media_item_id` BIGINT NULL ,
  PRIMARY KEY (`id`));

INSERT INTO media_item_rendition (rendition_id, filename, content_type, media_item_id)
 SELECT (SELECT id FROM rendition WHERE name LIKE "highRes"), media_item.filename, media_item.contentType, media_item.id FROM media_item;

CREATE TABLE `catalogue_rendition` (
  `catalogue_id` BIGINT NOT NULL ,
  `rendition_id` BIGINT NOT NULL ,
  PRIMARY KEY (`catalogue_id`, `rendition_id`) );

CREATE TABLE `catalogue_hook` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `execute_order` int(11) DEFAULT NULL,
  `hook_class` varchar(255) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `catalogue_id` bigint(20) DEFAULT NULL,
  `manual` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_catalogue_hook_catalogue` (`catalogue_id`)
);

CREATE TABLE `catalogue_hook_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `property_key` varchar(255) DEFAULT NULL,
  `property_value` varchar(255) DEFAULT NULL,
  `catalogue_hook_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_catalogue_hook_property` (`catalogue_hook_id`)
);


ALTER TABLE news_item_media_attachment DROP COLUMN opt_lock;
ALTER TABLE news_item_media_attachment ADD COLUMN display_order int(11) DEFAULT 0;

ALTER TABLE news_item_workflow_state_transition DROP COLUMN opt_lock;
ALTER TABLE news_item_workflow_state_transition ADD COLUMN submitted tinyint(1) DEFAULT '0';

ALTER TABLE workflow_step DROP COLUMN opt_lock;
ALTER TABLE workflow_step ADD COLUMN submitted tinyint(1) DEFAULT '0';

DROP TABLE `app_version`;

CREATE TABLE `app_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `from_version` varchar(255) NOT NULL,
  `to_version` varchar(255) NOT NULL,
  `migrated` tinyint(1) NOT NULL DEFAULT '0',
  `migrated_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

INSERT INTO `app_version` (from_version, to_version, migrated, migrated_date) VALUES ('', '1.0.8', 1, '2011-07-25');
INSERT INTO `app_version` (from_version, to_version) VALUES ('1.0.8', '1.0.9');
