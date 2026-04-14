-- Homework model refactor migration
-- Compatible with MySQL 5.7+/8+

ALTER TABLE homeworks ADD COLUMN content TEXT NULL;
ALTER TABLE homeworks ADD COLUMN start_time DATETIME NULL;
ALTER TABLE homeworks ADD COLUMN end_time DATETIME NULL;
ALTER TABLE homeworks ADD COLUMN allow_late_submit TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE homeworks ADD COLUMN total_score INT NOT NULL DEFAULT 100;
ALTER TABLE homeworks ADD COLUMN attachment_urls TEXT NULL;

UPDATE homeworks
SET content = COALESCE(content, description)
WHERE id > 0 AND content IS NULL;

UPDATE homeworks
SET end_time = COALESCE(end_time, deadline)
WHERE id > 0 AND end_time IS NULL;

UPDATE homeworks
SET start_time = COALESCE(start_time, created_at)
WHERE id > 0 AND start_time IS NULL;
