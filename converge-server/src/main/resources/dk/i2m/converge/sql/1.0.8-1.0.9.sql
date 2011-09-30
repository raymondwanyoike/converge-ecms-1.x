ALTER TABLE rendition DROP COLUMN opt_lock;
ALTER TABLE rendition ADD COLUMN label varchar(255);

ALTER TABLE news_item_media_attachment DROP COLUMN opt_lock;
ALTER TABLE news_item_media_attachment ADD COLUMN display_order int(11) DEFAULT 0;