
--
-- Upgrade Procedure 1.1.3 to 1.2.0
--

DELIMITER $$ 
DROP PROCEDURE IF EXISTS proc_upgrade_from_1_1_3_to_1_2_0$$ 

DELIMITER // 
CREATE PROCEDURE proc_upgrade_from_1_1_3_to_1_2_0() 
BEGIN 
    -- Add new columns
    call proc_add_column('newswire_service', 'copyright', 'TEXT DEFAULT');
    call proc_add_column('catalogue_hook', 'asynchronous', 'TINYINT(1) DEFAULT 0');
    call proc_add_column('catalogue', 'self_upload_state', 'BIGINT(20) DEFAULT NULL');

    -- Drop unnecessary columns
    call proc_drop_column('workflow', 'opt_lock');
    call proc_drop_column('workflow_state', 'opt_lock');
    call proc_drop_column('user_account', 'clearance_level');
    call proc_drop_column('user_account', 'opt_lock');
    call proc_drop_column('news_item', 'sub_position');
    call proc_drop_column('news_item', 'start_position');
    call proc_drop_column('news_item', 'edition_id');
    call proc_drop_column('news_item', 'edition_section_id');
    call proc_drop_column('news_item', 'department_id');
    call proc_drop_column('media_item', 'filename');
    call proc_drop_column('media_item', 'rendition_id');
    call proc_drop_column('media_item', 'contentType');

    call proc_1_2_0_content_item_migration();
    call proc_1_2_0_structures();
    call proc_1_2_0_migrate_catalogue_workflow();

END // 
DELIMITER ; 


DELIMITER $$ 
DROP PROCEDURE IF EXISTS proc_1_2_0_content_item_migration$$ 

DELIMITER // 
CREATE PROCEDURE proc_1_2_0_content_item_migration()
BEGIN
    CREATE TABLE IF NOT EXISTS `content_item` (
        `id` BIGINT NOT NULL AUTO_INCREMENT,
        `title` TEXT,
        `created` datetime DEFAULT NULL,
        `updated` datetime DEFAULT NULL,
        `current_state_id` BIGINT(20) DEFAULT NULL,
        `precalc_current_actor` VARCHAR(255) DEFAULT '',
        `content_type` VARCHAR(255) DEFAULT '',
        `location` VARCHAR(255) DEFAULT '',
        `thumbnail_link` TEXT,
        PRIMARY KEY (`id`)
    );

    ALTER TABLE `news_item_workflow_state_transition` CHANGE COLUMN `news_item_id` `content_item_id` BIGINT;
    
    ALTER TABLE `news_item_actor` RENAME TO `content_item_actor`;
    ALTER TABLE `news_item_workflow_state_transition` RENAME TO `content_item_workflow_transition`;
    ALTER TABLE `content_item_actor` DROP COLUMN `opt_lock`;
    ALTER TABLE `content_item_actor` CHANGE COLUMN `news_item_id` `content_item_id` BIGINT;

    -- Adding Many-To-Many relationship between Catalogue and UserRole
    CREATE TABLE IF NOT EXISTS catalogue_role (
        `catalogue_id` BIGINT NOT NULL,
        `role_id` BIGINT NOT NULL,
        PRIMARY KEY (`catalogue_id`,`role_id`),
        KEY `FK_catalogue_role_role_id` (`role_id`)
    );	

    INSERT INTO `content_item` (`id`, `title`, `created`, `updated`, `current_state_id`, `content_type`) SELECT `id`, `title`, `created`, `updated`, `current_state_id`, 'news_item' FROM `news_item`;
    UPDATE content_item c, news_item n, outlet o SET c.location=o.title WHERE n.outlet_id=o.id AND c.id=n.id;

    call proc_drop_column('news_item', 'current_state_id');
    call proc_drop_column('news_item', 'created');
    call proc_drop_column('news_item', 'title');
    call proc_drop_column('news_item', 'updated');

END //


DELIMITER $$ 
DROP PROCEDURE IF EXISTS proc_1_2_0_migrate_catalogue_workflow$$ 

DELIMITER // 
CREATE PROCEDURE proc_1_2_0_migrate_catalogue_workflow()
BEGIN
	DECLARE done INT DEFAULT FALSE;
	DECLARE cat_id, cat_editor_role_id, cat_user_role_id BIGINT DEFAULT 0; 
        DECLARE cat_name VARCHAR(255) DEFAULT '';
	DECLARE cur_catalogue CURSOR FOR SELECT `id`, `editor_role_id`, `user_role_id`, `name` FROM catalogue; 
        DECLARE cur_media_items CURSOR FOR SELECT `id`, `created`, `updated`, `status`, `owner` FROM media_item; 
	DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        -- Add new columns
        call proc_add_column('catalogue', 'workflow_id', 'BIGINT DEFAULT NULL');
        -- temporary column - deleted later
        call proc_add_column('media_item', 'current_state_id', 'BIGINT DEFAULT NULL');

	OPEN cur_catalogue;
	
	read_loop: LOOP
		FETCH cur_catalogue INTO cat_id, cat_editor_role_id, cat_user_role_id, cat_name;
	    IF done THEN
    	  LEAVE read_loop;
	    END IF;
	    
	    INSERT INTO `workflow` (`name`, `description`) VALUES (CONCAT(cat_name, ' ', 'workflow'), 'Basic catalogue workflow'); 
	    SELECT @workflow_id:=id AS id FROM `workflow` WHERE id = last_insert_id(); 
	    
	    INSERT INTO `workflow_state` (`state_name`, `state_description`, `display_order`, `permision`, `role`, `workflow_id`, `show_in_inbox`) VALUES ('Unsubmitted', 'Unsubmitted items', 1, 'USER', cat_user_role_id, @workflow_id, 1);
	    SELECT @unsubmitted_id:=id AS ID FROM `workflow_state` WHERE id = last_insert_id();
	    
	    INSERT INTO `workflow_state` (`state_name`, `state_description`, `display_order`, `permision`, `role`, `workflow_id`, `show_in_inbox`) VALUES ('Submitted', 'Submitted items', 2, 'GROUP', cat_editor_role_id, @workflow_id, 1);
	    SELECT @submitted_id:=id AS ID FROM `workflow_state` WHERE id = last_insert_id();
	    	    
	    INSERT INTO `workflow_state` (`state_name`, `state_description`, `display_order`, `permision`, `role`, `workflow_id`, `show_in_inbox`) VALUES ('Self-upload', 'Item uploaded by user', 3, 'GROUP', cat_editor_role_id, @workflow_id, 1);
            SELECT @selfupload_id:=id AS ID FROM `workflow_state` WHERE id = last_insert_id();
	    	    
	    INSERT INTO `workflow_state` (`state_name`, `state_description`, `display_order`, `permision`, `role`, `workflow_id`, `show_in_inbox`) VALUES ('Approved', 'Approved items', 5, 'GROUP', cat_editor_role_id, @workflow_id, 1);
   	    SELECT @approved_id:=id AS ID FROM `workflow_state` WHERE id = last_insert_id();

	    INSERT INTO `workflow_state` (`state_name`, `state_description`, `display_order`, `permision`, `role`, `workflow_id`, `show_in_inbox`) VALUES ('Trash', 'Trashed items', 0, 'USER', cat_user_role_id, @workflow_id, 0);
   	    SELECT @trash_id:=id AS ID FROM `workflow_state` WHERE id = last_insert_id();
	    
	    INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Submit to editor', 'Submit the media item to an editor for review', @unsubmitted_id, @submitted_id, 1, 1);
	    INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Return for revision', 'Return the submitted item to submitter', @submitted_id, @unsubmitted_id, 0, 1);
	    INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Approve', 'Approve the submitted item for archiving in the catalogue', @submitted_id, @approved_id, 0, 2);
            INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Reject', 'Reject the submitted item', @submitted_id, @trash_id, 0, 3);
            INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Return for revision', 'Return the self-uploaded item to submitter', @selfupload_id, @unsubmitted_id, 0, 1);	    
	    INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Approve', 'Approve the self-uploaded item for archiving in the catalogue', @selfupload_id, @approved_id, 0, 2);
	    INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Reject', 'Reject the self-uploaded item', @submitted_id, @selfupload_id, 0, 3);
	    INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Return for revision', 'Return the submitted item to submitter', @approved_id, @unsubmitted_id, 0, 1);
	    INSERT INTO `workflow_step` (`name`, `description`, `from_state_id`, `to_state_id`, `submitted`, `display_order`) VALUES ('Reject', 'Reject the submitted item', @approved_id, @trash_id, 0, 3);
	    
            UPDATE workflow SET workflow_state_start=@unsubmitted_id, workflow_state_end=@approved_id, workflow_state_trash=@trash_id WHERE id=@workflow_id;

            UPDATE catalogue SET workflow_id=@workflow_id WHERE id=cat_id;

            UPDATE media_item SET current_state_id = @unsubmitted_id WHERE catalogue_id=cat_id AND status LIKE 'UNSUBMITTED';
            UPDATE media_item SET current_state_id = @selfupload_id WHERE catalogue_id=cat_id AND status LIKE 'SELF_UPLOAD';
            UPDATE media_item SET current_state_id = @submitted_id WHERE catalogue_id=cat_id AND status LIKE 'SUBMITTED';
            UPDATE media_item SET current_state_id = @approved_id WHERE catalogue_id=cat_id AND status LIKE 'APPROVED';
            UPDATE media_item SET current_state_id = @trash_id WHERE catalogue_id=cat_id AND status LIKE 'REJECTED';

            INSERT INTO content_item_workflow_transition (content_item_id, transition_timestamp, state_id, user_account_id)
                SELECT `id`, `created`, @unsubmitted_id, `owner` FROM media_item WHERE catalogue_id=cat_id;
            INSERT INTO content_item_workflow_transition (content_item_id, transition_timestamp, state_id, user_account_id, submitted)
                SELECT `id`, `updated`, @submitted_id, `owner`, '1' FROM media_item WHERE catalogue_id=cat_id AND status LIKE 'SUBMITTED';
            INSERT INTO content_item_workflow_transition (content_item_id, transition_timestamp, state_id, user_account_id, submitted)
                SELECT `id`, `updated`, @approved_id, `owner`, '1' FROM media_item WHERE catalogue_id=cat_id AND status LIKE 'APPROVED';
            INSERT INTO content_item_workflow_transition (content_item_id, transition_timestamp, state_id, user_account_id, submitted)
                SELECT `id`, `updated`, @selfupload_id, `owner`, '1' FROM media_item WHERE catalogue_id=cat_id AND status LIKE 'SELF_UPLOAD';
            INSERT INTO content_item_workflow_transition (content_item_id, transition_timestamp, state_id, user_account_id, submitted)
                SELECT `id`, `updated`, @trash_id, `owner`, '1' FROM media_item WHERE catalogue_id=cat_id AND status LIKE 'REJECTED';


	END LOOP;
	
	CLOSE cur_catalogue;
        
        -- Remove fault items
        DELETE FROM media_item WHERE status IS NULL;

        -- Copy media items into content item table
        INSERT INTO `content_item` (`id`, `title`, `created`, `updated`, `current_state_id`, `content_type`) SELECT `id`, `title`, `created`, `updated`, `current_state_id`, 'media_item' FROM `media_item`;
        UPDATE content_item c, media_item m, catalogue t SET c.location=t.name WHERE m.catalogue_id=t.id AND c.id=m.id;
        call proc_drop_column('media_item', 'title');        
        call proc_drop_column('media_item', 'current_state_id');
        call proc_drop_column('media_item', 'status');
        call proc_drop_column('media_item', 'created');
        call proc_drop_column('media_item', 'updated');

        -- Remove catalogue fields no longer needed
        call proc_drop_column('catalogue', 'editor_role_id');
        call proc_drop_column('catalogue', 'user_role_id');
END //

DELIMITER $$ 
DROP PROCEDURE IF EXISTS proc_1_2_0_structures$$ 

DELIMITER // 
CREATE PROCEDURE proc_1_2_0_structures()
BEGIN

INSERT INTO `user_account` (`id`, `username`, `user_status`, `time_zone`, `dn`, `lang`, `employment_type`, `fee_type`, `full_name`, `given_name`, `surname`, `job_title`, `organisation`, `email`, `phone`, `mobile`, `default_outlet`, `default_section`, `default_assignment_type`, `default_media_repository`, `default_add_next_edition`, `default_search_engine_tags`, `default_search_results_order_by`, `default_search_results_order`, `default_work_day`)
VALUES
	(0, 'converge', '', NULL, NULL, 'en', 'UNKNOWN', 'UNKNOWN', 'Converge', 'Converge', '', '', '', 'noreply@getconverge.com', NULL, NULL, NULL, NULL, NULL, NULL, 0, 1, NULL, 0, 1);

CREATE TABLE IF NOT EXISTS  `news_item_property` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `news_item_id` int(11) DEFAULT NULL,
  `property_name` VARCHAR(255) DEFAULT '',
  `property_value` TEXT,
  `property_visibility` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS  `job_queue` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT '',
    `type_class` VARCHAR(255) DEFAULT '',
    `type_class_id` BIGINT DEFAULT NULL,
    `plugin_action_class` VARCHAR(255) DEFAULT '',
    `plugin_configuration_id` BIGINT DEFAULT NULL,
    `status` VARCHAR(255) DEFAULT '',
    `execution_time` datetime DEFAULT null,
    `started` datetime DEFAULT null,
    `finished` datetime DEFAULT null,
    `added` datetime DEFAULT null,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS  `job_queue_parameter` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `param_name` VARCHAR(255) DEFAULT '',
    `param_value` TEXT,
    `job_queue_id` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`)
);


CREATE TABLE IF NOT EXISTS  `plugin_configuration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) DEFAULT '',
    `description` TEXT,
    `action_class` VARCHAR(255) DEFAULT '',    
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS  `plugin_configuration_oncomplete` (
    `main_configuration_id` BIGINT NOT NULL,
    `complete_configuration_id` BIGINT NOT NULL,
    PRIMARY KEY (`main_configuration_id`, `complete_configuration_id`)
);


CREATE TABLE IF NOT EXISTS  `plugin_configuration_property` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `plugin_configuration_id` BIGINT DEFAULT NULL,
    `property_key` VARCHAR(255) DEFAULT '',
    `property_value` TEXT,
    PRIMARY KEY (`id`)
);

-- Drupal Client

CREATE TABLE IF NOT EXISTS  `news_item_edition_state` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `eid` int(11) DEFAULT NULL,
  `nid` int(11) DEFAULT NULL,
  `label` TEXT,
  `property` TEXT,
  `value` TEXT,
  `visible` tinyint(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- Changes to MediaItem

CREATE TABLE IF NOT EXISTS  `media_item_placement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `media_item_id` BIGINT DEFAULT NULL,
  `channel_id` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- Info pages

CREATE TABLE IF NOT EXISTS  `wiki_page` (
    `id` BIGINT NOT NULL AUTO_INCREMENT ,
    `show_submenu` tinyint(1) DEFAULT '0',
    `submenu_style` VARCHAR(255) DEFAULT '',
    `title` VARCHAR(255) DEFAULT '',
    `display_order` INT NULL,
    `page_content` TEXT DEFAULT '',
    `last_updater` bigint(20) DEFAULT NULL,
    `updated` datetime DEFAULT null,
    `created` datetime DEFAULT null,
    PRIMARY KEY (`id`)
);


END //




-- 
-- Utility procedures
--

DELIMITER $$ 
DROP PROCEDURE IF EXISTS proc_drop_column$$ 

DELIMITER // 
CREATE PROCEDURE proc_drop_column(tableName VARCHAR(255), columnName VARCHAR(255))
BEGIN
   	DECLARE _stmt VARCHAR(1024);
    SET @currentDb:=database();
    SET @sql := CONCAT('ALTER TABLE ', tableName, ' DROP COLUMN ', columnName);
    
    IF EXISTS (SELECT * FROM information_schema.COLUMNS WHERE COLUMN_NAME = columnName AND TABLE_NAME = tableName AND TABLE_SCHEMA = @currentDb) THEN 

    PREPARE _stmt FROM @sql;
    EXECUTE _stmt;
    DEALLOCATE PREPARE _stmt;

    END IF;

END //

DELIMITER $$ 
DROP PROCEDURE IF EXISTS proc_add_column$$ 

DELIMITER // 
CREATE PROCEDURE proc_add_column(tableName VARCHAR(255), columnName VARCHAR(255), columnDef VARCHAR(255))
BEGIN
   	DECLARE _stmt VARCHAR(1024);
    SET @currentDb:=database();
    SET @sql := CONCAT('ALTER TABLE ', tableName, ' ADD COLUMN ', columnName, ' ', columnDef);
    
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE COLUMN_NAME = columnName AND TABLE_NAME = tableName AND TABLE_SCHEMA = @currentDb) THEN 

    PREPARE _stmt FROM @sql;
    EXECUTE _stmt;
    DEALLOCATE PREPARE _stmt;

    END IF;

END //



call proc_upgrade_from_1_1_3_to_1_2_0();
