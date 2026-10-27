-- ============================================================
-- v4 升级脚本：富文本笔记 / 作业与试卷的开始时间 / 笔记关联业务对象
-- 适用：已经在跑的 online_learning 库（不删库，可重复执行）
-- 内容：
--   1. assignment / exam_paper 增加 start_time，「开始时间 ~ 截止时间」之外学生不能作答
--   2. study_note 增加 ref_type / ref_id，笔记可以挂在某次作业(ASSIGNMENT)或某张试卷(EXAM)上
--   3. 笔记内容放宽为 MEDIUMTEXT，容纳富文本（含粘贴图片）
-- 执行方式：mysql -u root -p --default-character-set=utf8mb4 online_learning < upgrade-v4.sql
-- ============================================================

USE online_learning;

DROP PROCEDURE IF EXISTS add_col;
DELIMITER $$
CREATE PROCEDURE add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(255))
BEGIN
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = col) THEN
        SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ---------------- 1. 开始时间 ----------------
CALL add_col('assignment', 'start_time',
             'start_time DATETIME NULL COMMENT ''开放作答时间，为空表示不限'' AFTER attach_url');
CALL add_col('exam_paper', 'start_time',
             'start_time DATETIME NULL COMMENT ''开放作答时间，为空表示不限'' AFTER duration');
CALL add_col('exam_paper', 'end_time',
             'end_time DATETIME NULL COMMENT ''关闭作答时间，为空表示不限'' AFTER start_time');

-- ---------------- 2. 笔记关联 ----------------
CALL add_col('study_note', 'ref_type',
             'ref_type VARCHAR(20) NULL COMMENT ''关联对象类型：ASSIGNMENT / EXAM，空表示普通笔记'' AFTER chapter_id');
CALL add_col('study_note', 'ref_id',
             'ref_id BIGINT NULL COMMENT ''关联对象 id'' AFTER ref_type');
ALTER TABLE study_note MODIFY COLUMN content MEDIUMTEXT COMMENT '笔记正文，支持富文本 HTML';

DROP PROCEDURE IF EXISTS add_col;

-- ---------------- 3. 存量数据兜底 ----------------
-- 历史作业没有开始时间，统一置为创建时间，避免出现「永远未开始」
UPDATE assignment SET start_time = create_time WHERE start_time IS NULL;
UPDATE exam_paper SET start_time = create_time WHERE start_time IS NULL;
SELECT 'v4 upgrade done' AS msg;
