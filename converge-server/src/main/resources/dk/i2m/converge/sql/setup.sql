
CREATE TABLE `announcement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `description` mediumtext,
  `published` tinyint(1) DEFAULT '0',
  `announcement_date` datetime DEFAULT NULL,
  `thumb` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `app_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `from_version` varchar(255) NOT NULL,
  `to_version` varchar(255) NOT NULL,
  `migrated` tinyint(1) NOT NULL DEFAULT '0',
  `migrated_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `assignment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(255) DEFAULT NULL,
  `assignment_briefing` mediumtext,
  `deadline` datetime DEFAULT NULL,
  `assigned_to` bigint(20) DEFAULT NULL,
  `assigned_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_assignment_assigned_to` (`assigned_to`),
  KEY `FK_assignment_assigned_by` (`assigned_by`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `background_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `task_start` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `catalogue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `last_index` datetime DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT '0',
  `watch_location` mediumtext,
  `read_only` tinyint(1) DEFAULT '0',
  `location` mediumtext,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `last_item_count` int(11) DEFAULT NULL,
  `web_access` mediumtext,
  `preview_rendition` bigint(20) DEFAULT NULL,
  `original_rendition` bigint(20) DEFAULT NULL,
  `max_file_upload_size` bigint(20) DEFAULT '0',
  `self_upload_state` bigint(20) DEFAULT NULL,
  `workflow_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `catalogue_hook` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `execute_order` int(11) DEFAULT NULL,
  `hook_class` varchar(255) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `catalogue_id` bigint(20) DEFAULT NULL,
  `manual` tinyint(1) DEFAULT '0',
  `asynchronous` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_catalogue_hook_catalogue` (`catalogue_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `catalogue_hook_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `property_key` varchar(255) DEFAULT NULL,
  `property_value` varchar(255) DEFAULT NULL,
  `catalogue_hook_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_catalogue_hook_property` (`catalogue_hook_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `catalogue_rendition` (
  `catalogue_id` bigint(20) NOT NULL,
  `rendition_id` bigint(20) NOT NULL,
  PRIMARY KEY (`catalogue_id`,`rendition_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `catalogue_role` (
  `catalogue_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`catalogue_id`,`role_id`),
  KEY `FK_catalogue_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `clearance_level` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `clearance_level` int(11) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `concept` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(20) DEFAULT NULL,
  `definition` mediumtext,
  `updated` datetime DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `poi_open_hours` varchar(255) DEFAULT NULL,
  `poi_contact_info` varchar(255) DEFAULT NULL,
  `poi_address_info` varchar(255) DEFAULT NULL,
  `poi_capacity` varchar(255) DEFAULT NULL,
  `poi_longitude` double DEFAULT NULL,
  `poi_latitude` double DEFAULT NULL,
  `org_founded` varchar(255) DEFAULT NULL,
  `org_dissolved` varchar(255) DEFAULT NULL,
  `person_born` varchar(255) DEFAULT NULL,
  `person_died` varchar(255) DEFAULT NULL,
  `geo_altitude` int(11) DEFAULT NULL,
  `geo_longitude` double DEFAULT NULL,
  `geo_latitude` double DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `FK_concept_updated_by` (`updated_by`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `concept_broader` (
  `broader_id` bigint(20) NOT NULL,
  `narrower_id` bigint(20) NOT NULL,
  PRIMARY KEY (`broader_id`,`narrower_id`),
  KEY `FK_concept_broader_narrower_id` (`narrower_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `concept_related` (
  `concept_id1` bigint(20) NOT NULL,
  `concept_id2` bigint(20) NOT NULL,
  PRIMARY KEY (`concept_id1`,`concept_id2`),
  KEY `FK_concept_related_concept_id2` (`concept_id2`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `concept_same_as` (
  `concept_id1` bigint(20) NOT NULL,
  `concept_id2` bigint(20) NOT NULL,
  PRIMARY KEY (`concept_id1`,`concept_id2`),
  KEY `FK_concept_same_as_concept_id2` (`concept_id2`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `config_value` mediumtext,
  `config_key` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNQ_config_0` (`config_key`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `contact` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) NOT NULL DEFAULT '',
  `last_name` varchar(255) NOT NULL DEFAULT '',
  `organisation` text,
  `note` text,
  `job_title` varchar(255) NOT NULL DEFAULT '',
  `title` varchar(255) NOT NULL DEFAULT '',
  `created` datetime NOT NULL,
  `updated` datetime NOT NULL,
  `created_by` bigint(20) NOT NULL,
  `updated_by` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_contact_updated_by` (`updated_by`),
  KEY `FK_contact_created_by` (`created_by`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `contact_address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `display_order` int(11) DEFAULT NULL,
  `label` varchar(255) NOT NULL DEFAULT '',
  `address_line1` varchar(255) NOT NULL DEFAULT '',
  `address_line2` varchar(255) NOT NULL DEFAULT '',
  `city` varchar(255) NOT NULL DEFAULT '',
  `country` varchar(255) NOT NULL DEFAULT '',
  `address_state` varchar(255) NOT NULL DEFAULT '',
  `contact_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_contact_address_contact_id` (`contact_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `contact_email` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `display_order` int(11) NOT NULL,
  `label` varchar(255) NOT NULL,
  `email` text,
  `contact_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_contact_email_contact_id` (`contact_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `contact_phone` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `display_order` int(11) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `number` varchar(255) DEFAULT NULL,
  `contact_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_contact_phone_contact_id` (`contact_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `content_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` text,
  `created` datetime DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  `current_state_id` bigint(20) DEFAULT NULL,
  `precalc_current_actor` varchar(255) DEFAULT '',
  `content_type` varchar(255) DEFAULT '',
  `location` varchar(255) DEFAULT '',
  `thumbnail_link` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `content_item_actor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content_item_id` bigint(20) DEFAULT NULL,
  `role_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_news_item_actor_user_id` (`user_id`),
  KEY `FK_news_item_actor_news_item_id` (`content_item_id`),
  KEY `FK_news_item_actor_role_id` (`role_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `content_item_workflow_transition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `transition_timestamp` datetime DEFAULT NULL,
  `brief_version` mediumtext,
  `story_version` mediumtext,
  `comment` mediumtext,
  `headline_version` mediumtext,
  `content_item_id` bigint(20) DEFAULT NULL,
  `state_id` bigint(20) DEFAULT NULL,
  `user_account_id` bigint(20) DEFAULT NULL,
  `submitted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `news_item_workflow_state_transition_news_item_id` (`content_item_id`),
  KEY `FK_news_item_workflow_state_transition_state_id` (`state_id`),
  KEY `news_item_workflow_state_transitionuser_account_id` (`user_account_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `content_language` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `content_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `department` (
  `id` bigint(20) NOT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '0',
  `outlet_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_department_outlet_id` (`outlet_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `department_membership` (
  `department_id` bigint(20) NOT NULL,
  `user_account_id` bigint(20) NOT NULL,
  PRIMARY KEY (`department_id`,`user_account_id`),
  KEY `FK_department_membership_user_account_id` (`user_account_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `department_role` (
  `department_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`department_id`,`role_id`),
  KEY `FK_department_role_role_id` (`role_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `edition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `open_for_input` tinyint(1) DEFAULT '0',
  `expiration_date` datetime DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `volume` int(11) DEFAULT NULL,
  `number` int(11) DEFAULT NULL,
  `publication_date` datetime DEFAULT NULL,
  `outlet_id` bigint(20) DEFAULT NULL,
  `close_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_edition_outlet_id` (`outlet_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `edition_pattern` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `start_minute` int(11) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `active_to` datetime DEFAULT NULL,
  `end_minute` int(11) DEFAULT NULL,
  `start_hour` int(11) DEFAULT NULL,
  `edition_date` int(11) DEFAULT NULL,
  `close_minute` int(11) DEFAULT NULL,
  `end_hour` int(11) DEFAULT NULL,
  `active_from` datetime DEFAULT NULL,
  `close_hour` int(11) DEFAULT NULL,
  `outlet_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_edition_pattern_outlet_id` (`outlet_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `edition_section` (
  `id` bigint(20) NOT NULL,
  `section_start` int(11) DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `is_advertisement` tinyint(1) DEFAULT '0',
  `section_end` int(11) DEFAULT NULL,
  `edition_id` bigint(20) DEFAULT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_edition_section_parent_id` (`parent_id`),
  KEY `FK_edition_section_edition_id` (`edition_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `edition_template` (
  `id` bigint(20) NOT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `outlet_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_edition_template_outlet_id` (`outlet_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `edition_template_section` (
  `id` bigint(20) NOT NULL,
  `section_start` int(11) DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `is_advertisement` tinyint(1) DEFAULT '0',
  `section_end` int(11) DEFAULT NULL,
  `template_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_edition_template_section_template_id` (`template_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `summary` varchar(255) DEFAULT NULL,
  `start_date` datetime DEFAULT NULL,
  `all_day_event` tinyint(1) DEFAULT '0',
  `category` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `description` mediumtext,
  `end_date` datetime DEFAULT NULL,
  `originator_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_event_originator_id` (`originator_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `event_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `opt_lock` int(11) DEFAULT NULL,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `event_news_item` (
  `Event_id` bigint(20) NOT NULL,
  `newsItem_id` bigint(20) NOT NULL,
  PRIMARY KEY (`Event_id`,`newsItem_id`),
  KEY `FK_event_news_item_newsItem_id` (`newsItem_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `financial_market` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '0',
  `short_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `forex_currency` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `symbol` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '0',
  `short_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `forex_rate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `updated` datetime DEFAULT NULL,
  `currency_change` double DEFAULT NULL,
  `currency_value` double DEFAULT NULL,
  `currency` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_forex_rate_currency` (`currency`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `index_queue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entry_type` varchar(255) DEFAULT NULL,
  `entry_id` bigint(20) DEFAULT NULL,
  `added` datetime DEFAULT NULL,
  `entry_operation` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `job_queue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT '',
  `type_class` varchar(255) DEFAULT '',
  `type_class_id` bigint(20) DEFAULT NULL,
  `plugin_action_class` varchar(255) DEFAULT '',
  `plugin_configuration_id` bigint(20) DEFAULT NULL,
  `status` varchar(255) DEFAULT '',
  `execution_time` datetime DEFAULT NULL,
  `started` datetime DEFAULT NULL,
  `finished` datetime DEFAULT NULL,
  `added` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `job_queue_parameter` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `param_name` varchar(255) DEFAULT '',
  `param_value` text,
  `job_queue_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log_entry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` text,
  `severity` varchar(255) DEFAULT '',
  `log_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_severity` (`severity`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `log_subject` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `entity` varchar(255) DEFAULT '',
  `entity_id` varchar(255) DEFAULT '',
  `link` varchar(255) DEFAULT '',
  `log_entry_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_entity` (`entity`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `market_value` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `updated` datetime DEFAULT NULL,
  `market_change` double DEFAULT NULL,
  `market_value` double DEFAULT NULL,
  `financial_market_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_market_value_financial_market_id` (`financial_market_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `media_item` (
  `id` bigint(20) NOT NULL,
  `media_type` varchar(10) DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `description` mediumtext,
  `catalogue_id` bigint(20) DEFAULT NULL,
  `media_date` datetime DEFAULT NULL,
  `editorial_note` mediumtext,
  `assignment_id` bigint(20) DEFAULT NULL,
  `owner` bigint(20) DEFAULT NULL,
  `byline` mediumtext,
  `held` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_media_item_media_repository_id` (`catalogue_id`),
  KEY `FK_media_item_assignment_id` (`assignment_id`),
  KEY `FK_media_item_owner` (`owner`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `media_item_concept` (
  `media_item_id` bigint(20) NOT NULL,
  `concept_id` bigint(20) NOT NULL,
  PRIMARY KEY (`media_item_id`,`concept_id`),
  KEY `FK_media_item_concept_concept_id` (`concept_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `media_item_placement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `media_item_id` bigint(20) DEFAULT NULL,
  `channel_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `media_item_rendition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `rendition_id` bigint(20) DEFAULT NULL,
  `filename` text,
  `path` text,
  `content_type` varchar(255) DEFAULT '',
  `file_size` bigint(20) DEFAULT '0',
  `width` int(11) DEFAULT NULL,
  `height` int(11) DEFAULT NULL,
  `colourSpace` varchar(255) DEFAULT '',
  `resolution` int(11) DEFAULT NULL,
  `audio_bitrate` int(11) DEFAULT NULL,
  `audio_channels` varchar(255) DEFAULT '',
  `audio_codec` varchar(255) DEFAULT '',
  `audio_sample_size` int(11) DEFAULT NULL,
  `audio_sample_rate` int(11) DEFAULT NULL,
  `audio_variable_bitrate` tinyint(1) DEFAULT '0',
  `duration` int(11) DEFAULT NULL,
  `video_codec` varchar(255) DEFAULT '',
  `video_average_bit_rate` int(11) DEFAULT NULL,
  `video_variable_bit_rate` tinyint(1) DEFAULT '0',
  `video_frame_rate` int(11) DEFAULT NULL,
  `video_scan_technique` varchar(255) DEFAULT '',
  `video_aspect_ratio` varchar(255) DEFAULT '',
  `video_sampling_method` varchar(255) DEFAULT '',
  `media_item_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `news_item` (
  `id` bigint(20) NOT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `story` mediumtext,
  `assigned` tinyint(1) DEFAULT '0',
  `by_line` varchar(255) DEFAULT NULL,
  `undisclosed` tinyint(1) DEFAULT '0',
  `deadline` datetime DEFAULT NULL,
  `assignment_briefing` mediumtext,
  `brief` mediumtext,
  `outlet_id` bigint(20) DEFAULT NULL,
  `assigned_by` bigint(20) DEFAULT NULL,
  `event_id` bigint(20) DEFAULT NULL,
  `version_news_item_id` bigint(20) DEFAULT NULL,
  `target_word_count` int(11) NOT NULL DEFAULT '0',
  `checked_out` datetime DEFAULT NULL,
  `checked_out_by` bigint(20) DEFAULT NULL,
  `section_id` bigint(20) DEFAULT NULL,
  `assignment_id` bigint(20) DEFAULT NULL,
  `language_id` bigint(20) DEFAULT NULL,
  `slugline` varchar(255) DEFAULT '',
  `precalc_word_count` bigint(20) DEFAULT '0',
  `precalc_current_actor` varchar(255) DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `FK_news_item_event_id` (`event_id`),
  KEY `FK_news_item_version_news_item_id` (`version_news_item_id`),
  KEY `FK_news_item_assigned_by` (`assigned_by`),
  KEY `FK_news_item_outlet_id` (`outlet_id`),
  KEY `FK_news_item_checked_out_by` (`checked_out_by`),
  KEY `FK_news_item_section_id` (`section_id`),
  KEY `FK_news_item_assignment_id` (`assignment_id`),
  KEY `FK_news_item_language_id` (`language_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `news_item_concept` (
  `news_item_id` bigint(20) NOT NULL,
  `concept_id` bigint(20) NOT NULL,
  PRIMARY KEY (`news_item_id`,`concept_id`),
  KEY `FK_news_item_concept_concept_id` (`concept_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `news_item_edition_state` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `eid` int(11) DEFAULT NULL,
  `nid` int(11) DEFAULT NULL,
  `label` text,
  `property` text,
  `value` text,
  `visible` tinyint(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `news_item_media_attachment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `caption` mediumtext,
  `media_item_id` bigint(20) DEFAULT NULL,
  `news_item_id` bigint(20) DEFAULT NULL,
  `display_order` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_news_item_media_attachment_news_item_id` (`news_item_id`),
  KEY `FK_news_item_media_attachment_media_item_id` (`media_item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `news_item_news_item_review_comment` (
  `NewsItem_id` bigint(20) NOT NULL,
  `shareComments_id` bigint(20) NOT NULL,
  PRIMARY KEY (`NewsItem_id`,`shareComments_id`),
  KEY `news_item_news_item_review_commentshareComments_id` (`shareComments_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `news_item_photo` (
  `news_item_id` bigint(20) NOT NULL,
  `photo_id` bigint(20) NOT NULL,
  PRIMARY KEY (`news_item_id`,`photo_id`),
  KEY `FK_news_item_photo_photo_id` (`photo_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `news_item_placement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sub_position` int(11) DEFAULT NULL,
  `start_position` int(11) DEFAULT NULL,
  `edition_id` bigint(20) DEFAULT NULL,
  `outlet_id` bigint(20) DEFAULT NULL,
  `news_item_id` bigint(20) DEFAULT NULL,
  `section_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_news_item_placement_news_item_id` (`news_item_id`),
  KEY `FK_news_item_placement_edition_id` (`edition_id`),
  KEY `FK_news_item_placement_section_id` (`section_id`),
  KEY `FK_news_item_placement_outlet_id` (`outlet_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `news_item_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `news_item_id` int(11) DEFAULT NULL,
  `property_name` varchar(255) DEFAULT '',
  `property_value` text,
  `property_visibility` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `news_item_related` (
  `news_item_id` bigint(20) NOT NULL,
  `related_news_item_id` bigint(20) NOT NULL,
  PRIMARY KEY (`news_item_id`,`related_news_item_id`),
  KEY `FK_news_item_related_related_news_item_id` (`related_news_item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `news_item_review_comment` (
  `id` bigint(20) NOT NULL,
  `posted_on` datetime DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `comment` mediumtext,
  `news_item_id` bigint(20) DEFAULT NULL,
  `comment_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_news_item_review_comment_comment_by` (`comment_by`),
  KEY `FK_news_item_review_comment_news_item_id` (`news_item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `news_item_share` (
  `news_item_id` bigint(20) NOT NULL,
  `user_account_id` bigint(20) NOT NULL,
  PRIMARY KEY (`news_item_id`,`user_account_id`),
  KEY `FK_news_item_share_user_account_id` (`user_account_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `newswire_basket` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `next_delivery` datetime DEFAULT NULL,
  `first_delivery_hour` int(11) DEFAULT NULL,
  `mail_delivery` tinyint(1) DEFAULT '0',
  `mail_frequency` int(11) DEFAULT NULL,
  `last_delivery` datetime DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `search_term` mediumtext,
  `created` datetime DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  `owner` bigint(20) DEFAULT NULL,
  `any_tags` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `FK_newswire_basket_owner` (`owner`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `newswire_baskets_tags` (
  `content_tag_id` bigint(20) NOT NULL,
  `newswire_basket_id` bigint(20) NOT NULL,
  PRIMARY KEY (`content_tag_id`,`newswire_basket_id`),
  KEY `FK_newswire_baskets_tags_newswire_basket_id` (`newswire_basket_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `newswire_item` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` mediumtext,
  `summary` mediumtext,
  `title` mediumtext,
  `updated` datetime DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `url` mediumtext,
  `newswire_service_id` bigint(20) DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `thumbnail_url` mediumtext,
  PRIMARY KEY (`id`),
  KEY `FK_newswire_item_newswire_service_id` (`newswire_service_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `newswire_item_attachment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` mediumtext,
  `name` mediumtext,
  `file_binary_data` longblob,
  `file_name` varchar(255) DEFAULT NULL,
  `file_content_type` varchar(255) DEFAULT NULL,
  `file_size` bigint(20) DEFAULT NULL,
  `newswire_item_id` bigint(20) DEFAULT NULL,
  `catalogue_id` bigint(20) DEFAULT NULL,
  `catalogue_path` mediumtext,
  `rendition_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_newswire_item_attachment_newswire_item_id` (`newswire_item_id`),
  KEY `FK_newswire_item_attachment_catalogue` (`catalogue_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `newswire_item_tag` (
  `content_tag_id` bigint(20) NOT NULL,
  `newswire_item_id` bigint(20) NOT NULL,
  PRIMARY KEY (`content_tag_id`,`newswire_item_id`),
  KEY `FK_newswire_item_tag_newswire_item_id` (`newswire_item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `newswire_restriction` (
  `user_role_id` bigint(20) NOT NULL,
  `newswire_service_id` bigint(20) NOT NULL,
  PRIMARY KEY (`user_role_id`,`newswire_service_id`),
  KEY `FK_newswire_restriction_newswire_service_id` (`newswire_service_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `newswire_service` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `source` varchar(255) DEFAULT NULL,
  `last_fetch` datetime DEFAULT NULL,
  `decoder_class` mediumtext,
  `active` tinyint(4) DEFAULT '0',
  `days_to_keep` int(11) DEFAULT '0',
  `processing` tinyint(1) DEFAULT '0',
  `copyright` text,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `newswire_service_baskets` (
  `newswire_basket_id` bigint(20) NOT NULL,
  `newswire_service_id` bigint(20) NOT NULL,
  PRIMARY KEY (`newswire_basket_id`,`newswire_service_id`),
  KEY `FK_newswire_service_baskets_newswire_service_id` (`newswire_service_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `newswire_service_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `opt_lock` int(11) DEFAULT NULL,
  `property_value` mediumtext,
  `property_key` varchar(255) DEFAULT NULL,
  `newswire_service_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_newswire_service_property_newswire_service_id` (`newswire_service_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `notification` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `message` mediumtext,
  `added` datetime DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `recipient_id` bigint(20) DEFAULT NULL,
  `link` varchar(255) DEFAULT NULL,
  `sender_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_notification_recipient_id` (`recipient_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `open_calais_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type_group` varchar(255) NOT NULL,
  `field` varchar(255) NOT NULL,
  `field_value` varchar(255) NOT NULL,
  `concept_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `organisation_location` (
  `location_id` bigint(20) NOT NULL,
  `organisation_id` bigint(20) NOT NULL,
  PRIMARY KEY (`location_id`,`organisation_id`),
  KEY `FK_organisation_location_organisation_id` (`organisation_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `outlet` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `workflow_id` bigint(20) DEFAULT NULL,
  `language_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_outlet_workflow_id` (`workflow_id`),
  KEY `FK_outlet_language_id` (`language_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `outlet_edition_action` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `execute_order` int(11) DEFAULT NULL,
  `action_class` varchar(255) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `outlet_id` bigint(20) DEFAULT NULL,
  `manual_action` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_outlet_edition_action_outlet_id` (`outlet_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `outlet_edition_action_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `opt_lock` int(11) DEFAULT NULL,
  `property_value` mediumtext,
  `property_key` varchar(255) DEFAULT NULL,
  `outlet_edition_action_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `outleteditionactionpropertyoutletedition_action_id` (`outlet_edition_action_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `outlet_role` (
  `outlet_id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`outlet_id`,`role_id`),
  KEY `FK_outlet_role_role_id` (`role_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `outlet_section` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `opt_lock` int(11) DEFAULT NULL,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '0',
  `outlet_id` bigint(20) DEFAULT NULL,
  `parent_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_outlet_section_parent_id` (`parent_id`),
  KEY `FK_outlet_section_outlet_id` (`outlet_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `outlet_subscriber` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `subscription_date` datetime DEFAULT NULL,
  `unsubscription_date` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT '',
  `email` varchar(255) DEFAULT '',
  `phone` varchar(255) DEFAULT '',
  `date_of_birth` datetime DEFAULT NULL,
  `location` varchar(255) DEFAULT '',
  `source` varchar(255) DEFAULT '',
  `subscribed` tinyint(1) DEFAULT '0',
  `outlet_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `person_affiliation` (
  `person_id` bigint(20) NOT NULL,
  `organisation_id` bigint(20) NOT NULL,
  PRIMARY KEY (`person_id`,`organisation_id`),
  KEY `FK_person_affiliation_organisation_id` (`organisation_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `photo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `photo_taken` date DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `description` mediumtext,
  `photographer_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_photo_photographer_id` (`photographer_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `photo_concept` (
  `Photo_id` bigint(20) NOT NULL,
  `concepts_id` bigint(20) NOT NULL,
  PRIMARY KEY (`Photo_id`,`concepts_id`),
  KEY `FK_photo_concept_concepts_id` (`concepts_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `photo_entry` (
  `id` bigint(20) NOT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `width` int(11) DEFAULT NULL,
  `heiht_inches` float DEFAULT NULL,
  `width_inches` float DEFAULT NULL,
  `file_binary_data` longblob,
  `format` varchar(255) DEFAULT NULL,
  `file_content_type` varchar(255) DEFAULT NULL,
  `width_dpi` int(11) DEFAULT NULL,
  `file_size` bigint(20) DEFAULT NULL,
  `height` int(11) DEFAULT NULL,
  `bits` int(11) DEFAULT NULL,
  `height_dpi` int(11) DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `photo_entry_id` varchar(255) DEFAULT NULL,
  `photo_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_photo_entry_photo_id` (`photo_id`),
  KEY `FK_photo_entry_photo_entry_id` (`photo_entry_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `photo_entry_type` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `photo_related` (
  `photo_id` bigint(20) NOT NULL,
  `related_photo_id` bigint(20) NOT NULL,
  PRIMARY KEY (`photo_id`,`related_photo_id`),
  KEY `FK_photo_related_related_photo_id` (`related_photo_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `PHOTOITEM` (
  `id` bigint(20) NOT NULL,
  `photo_taken` date DEFAULT NULL,
  `photographer_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_PHOTOITEM_photographer_id` (`photographer_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `plugin_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT '',
  `description` text,
  `action_class` varchar(255) DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `plugin_configuration_oncomplete` (
  `main_configuration_id` bigint(20) NOT NULL,
  `complete_configuration_id` bigint(20) NOT NULL,
  PRIMARY KEY (`main_configuration_id`,`complete_configuration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `plugin_configuration_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `plugin_configuration_id` bigint(20) DEFAULT NULL,
  `property_key` varchar(255) DEFAULT '',
  `property_value` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `privilege` (
  `id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `rendition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `default_height` int(11) DEFAULT '0',
  `default_width` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `SEQUENCE` (
  `SEQ_NAME` varchar(50) NOT NULL,
  `SEQ_COUNT` decimal(38,0) DEFAULT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `user_account` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `user_status` varchar(255) DEFAULT NULL,
  `time_zone` varchar(255) DEFAULT NULL,
  `dn` text,
  `lang` varchar(255) DEFAULT 'EN',
  `employment_type` varchar(15) DEFAULT 'UNKNOWN',
  `fee_type` varchar(15) DEFAULT 'UNKNOWN',
  `full_name` varchar(255) DEFAULT NULL,
  `given_name` varchar(255) DEFAULT NULL,
  `surname` varchar(255) DEFAULT NULL,
  `job_title` varchar(255) DEFAULT NULL,
  `organisation` varchar(255) DEFAULT NULL,
  `email` text,
  `phone` varchar(255) DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `default_outlet` bigint(20) DEFAULT NULL,
  `default_section` bigint(20) DEFAULT NULL,
  `default_assignment_type` varchar(255) DEFAULT NULL,
  `default_media_repository` bigint(20) DEFAULT NULL,
  `default_add_next_edition` tinyint(4) DEFAULT '0',
  `default_search_engine_tags` tinyint(4) DEFAULT '1',
  `default_search_results_order_by` varchar(255) DEFAULT NULL,
  `default_search_results_order` tinyint(1) NOT NULL DEFAULT '0',
  `default_work_day` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UNQ_user_account_0` (`username`),
  KEY `FK_user_account_default_outlet` (`default_outlet`),
  KEY `FK_user_account_default_section` (`default_section`),
  KEY `FK_user_account_default_media_repository` (`default_media_repository`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `user_account_membership` (
  `user_account_id` bigint(20) NOT NULL,
  `user_role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`user_account_id`,`user_role_id`),
  KEY `FK_user_account_membership_user_role_id` (`user_role_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `user_account_newswire_services` (
  `user_account_id` bigint(20) NOT NULL,
  `newswire_service_id` bigint(20) NOT NULL,
  PRIMARY KEY (`user_account_id`,`newswire_service_id`),
  KEY `user_account_newswire_services_newswire_service_id` (`newswire_service_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` mediumtext,
  `role_name` varchar(255) DEFAULT NULL,
  `dn` mediumtext,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `user_role_privilege` (
  `role_id` bigint(20) NOT NULL,
  `privilege_id` varchar(255) NOT NULL,
  PRIMARY KEY (`role_id`,`privilege_id`),
  KEY `FK_user_role_privilege_privilege_id` (`privilege_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `weather_forecast` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `updated` datetime DEFAULT NULL,
  `high` int(11) DEFAULT NULL,
  `low` int(11) DEFAULT NULL,
  `location` bigint(20) DEFAULT NULL,
  `situation` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_weather_forecast_situation` (`situation`),
  KEY `FK_weather_forecast_location` (`location`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `weather_location` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `weather_situation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `figure_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `wiki_page` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `show_submenu` tinyint(1) DEFAULT '0',
  `submenu_style` varchar(255) DEFAULT '',
  `title` varchar(255) DEFAULT '',
  `display_order` int(11) DEFAULT NULL,
  `page_content` text,
  `last_updater` bigint(20) DEFAULT NULL,
  `updated` datetime DEFAULT NULL,
  `created` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `workflow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `workflow_state_start` bigint(20) DEFAULT NULL,
  `workflow_state_trash` bigint(20) DEFAULT NULL,
  `workflow_state_end` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_workflow_workflow_state_start` (`workflow_state_start`),
  KEY `FK_workflow_workflow_state_trash` (`workflow_state_trash`),
  KEY `FK_workflow_workflow_state_end` (`workflow_state_end`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `workflow_state` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `display_order` int(11) DEFAULT NULL,
  `state_description` varchar(255) DEFAULT NULL,
  `state_name` varchar(255) DEFAULT NULL,
  `permision` varchar(255) DEFAULT NULL,
  `role` bigint(20) DEFAULT NULL,
  `workflow_id` bigint(20) NOT NULL,
  `department_assigned` tinyint(1) DEFAULT '0',
  `pullback_enabled` tinyint(1) DEFAULT '0',
  `show_in_inbox` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `FK_workflow_state_role` (`role`),
  KEY `FK_workflow_state_workflow_id` (`workflow_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `workflow_state_visible` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `field` varchar(255) DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `workflow_state_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_workflow_state_visible_workflow_state_id` (`workflow_state_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `workflow_step` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `display_order` int(11) DEFAULT NULL,
  `description` mediumtext,
  `name` varchar(255) DEFAULT NULL,
  `from_state_id` bigint(20) DEFAULT NULL,
  `to_state_id` bigint(20) DEFAULT NULL,
  `submitted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `FK_workflow_step_from_state_id` (`from_state_id`),
  KEY `FK_workflow_step_to_state_id` (`to_state_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `workflow_step_action` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `execute_order` int(11) DEFAULT NULL,
  `action_class` varchar(255) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `workflow_step` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_workflow_step_action_workflow_step` (`workflow_step`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `workflow_step_action_property` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `opt_lock` int(11) DEFAULT NULL,
  `property_value` mediumtext,
  `property_key` varchar(255) DEFAULT NULL,
  `workflow_step_action_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `workflowstepaction_propertyworkflow_step_action_id` (`workflow_step_action_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `workflow_step_user_role` (
  `workflow_step_id` bigint(20) NOT NULL,
  `user_role_id` bigint(20) NOT NULL,
  PRIMARY KEY (`workflow_step_id`,`user_role_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `workflow_step_validation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `field` varchar(255) DEFAULT NULL,
  `opt_lock` int(11) DEFAULT NULL,
  `workflow_step_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_workflow_step_validation_workflow_step_id` (`workflow_step_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `workflow_step_validator` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `workflow_step_id` bigint(20) DEFAULT NULL,
  `execute_order` int(11) NOT NULL DEFAULT '1',
  `label` varchar(255) NOT NULL DEFAULT '',
  `validator_class` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `workflow_step_validator_property` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `workflow_step_validator_id` bigint(20) DEFAULT NULL,
  `property_key` varchar(255) DEFAULT NULL,
  `property_value` text,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


# Default data

INSERT INTO `app_version` (`id`, `from_version`, `to_version`, `migrated`, `migrated_date`) VALUES (1,'','1.2.0-B12',1,'2012-08-12 22:15:00');
INSERT INTO `SEQUENCE` (`SEQ_NAME`, `SEQ_COUNT`) VALUES ('SEQ_GEN', 0);

INSERT INTO rendition (name, label, description) VALUES ('rnd:thumbnail', 'Thumbnail', 'A very small rendition of an image, giving only a general idea of its content.');
INSERT INTO rendition (name, label, description) VALUES ('rnd:preview', 'Preview', 'Preview resolution image or video');
INSERT INTO rendition (name, label, description) VALUES ('rnd:lowRes', 'Low resolution', 'Low resolution image or video');
INSERT INTO rendition (name, label, description) VALUES ('rnd:highRes', 'High resolution', 'High resolution image or video');
INSERT INTO rendition (name, label, description) VALUES ('rnd:print', 'Content for print', 'Content intended to appear in print');
INSERT INTO rendition (name, label, description) VALUES ('rnd:web', 'Content for a web page', 'Content intended to appear on a web page');
INSERT INTO rendition (name, label, description) VALUES ('rnd:sms', 'Content for short message', 'Content intended to appear in a short messaging system');
INSERT INTO rendition (name, label, description) VALUES ('rnd:mobile', 'Content for a mobile device', 'Content intended to appear on a mobile or handheld device');

INSERT INTO `user_account` (`id`, `username`, `user_status`, `time_zone`, `dn`, `lang`, `employment_type`, `fee_type`, `full_name`, `given_name`, `surname`, `job_title`, `organisation`, `email`, `phone`, `mobile`, `default_outlet`, `default_section`, `default_assignment_type`, `default_media_repository`, `default_add_next_edition`, `default_search_engine_tags`, `default_search_results_order_by`, `default_search_results_order`, `default_work_day`) VALUES (0, 'converge', '', NULL, NULL, 'EN', 'UNKNOWN', 'UNKNOWN', 'Converge', 'Converge', '', '', '', 'noreply@getconverge.com', NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, NULL, 0, 1);
