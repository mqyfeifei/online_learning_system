-- ============================================================
-- v5 升级脚本：学号/职工号登录 · 试卷发布 · 作业测验挂章节 · 讨论区「有帮助」
-- 适用：已经在跑的 online_learning 库（不删库，可重复执行）
-- 内容：
--   1. sys_user 增加 user_no（学号/职工号，唯一），登录改用 user_no
--   2. discussion_post 增加 helpful_count，新增 discussion_helpful 记录「谁点过有帮助」
--   3. exam_paper 增加 publish_status / published_at / version（发布机制）
--   4. exam_record 增加 paper_version（区分「哪一轮发布的答卷」）
--   5. assignment / exam_paper 增加 chapter_id（可空，作业测验可精准到章节）
-- 执行方式：mysql -u root -p --default-character-set=utf8mb4 online_learning < upgrade-v5.sql
-- ============================================================

USE online_learning;

DROP PROCEDURE IF EXISTS add_col;
DROP PROCEDURE IF EXISTS add_idx;
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
CREATE PROCEDURE add_idx(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl VARCHAR(255))
BEGIN
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx) THEN
        SET @s = CONCAT('ALTER TABLE ', tbl, ' ADD ', ddl);
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ---------------- 1. 学号 / 职工号 ----------------
CALL add_col('sys_user', 'user_no',
             'user_no VARCHAR(32) NULL COMMENT ''学号/职工号，登录用，全平台唯一'' AFTER username');

-- 存量账号回填：按角色前缀 + 6 位 id，保证唯一（改完可在后台再改）
UPDATE sys_user SET user_no = CONCAT('A', LPAD(id, 6, '0')) WHERE (user_no IS NULL OR user_no = '') AND role = 'ADMIN';
UPDATE sys_user SET user_no = CONCAT('T', LPAD(id, 6, '0')) WHERE (user_no IS NULL OR user_no = '') AND role = 'TEACHER';
UPDATE sys_user SET user_no = CONCAT('S', LPAD(id, 6, '0')) WHERE (user_no IS NULL OR user_no = '') AND role = 'STUDENT';
UPDATE sys_user SET user_no = CONCAT('U', LPAD(id, 6, '0')) WHERE user_no IS NULL OR user_no = '';

CALL add_idx('sys_user', 'uk_user_no', 'UNIQUE KEY uk_user_no (user_no)');

-- ---------------- 2. 讨论区「有帮助」 ----------------
CALL add_col('discussion_post', 'helpful_count',
             'helpful_count INT NOT NULL DEFAULT 0 COMMENT ''有帮助数（每人只能点一次）'' AFTER like_count');
UPDATE discussion_post SET helpful_count = like_count WHERE helpful_count = 0 AND like_count > 0;

CREATE TABLE IF NOT EXISTS discussion_helpful (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    post_id    BIGINT   NOT NULL COMMENT '帖子',
    user_id    BIGINT   NOT NULL COMMENT '点「有帮助」的人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_user (post_id, user_id),
    KEY idx_post (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '帖子「有帮助」去重表';

-- ---------------- 3. 试卷发布机制 ----------------
CALL add_col('exam_paper', 'chapter_id',
             'chapter_id BIGINT NULL COMMENT ''所属章节，空表示整门课'' AFTER course_id');
CALL add_col('exam_paper', 'publish_status',
             'publish_status TINYINT NOT NULL DEFAULT 0 COMMENT ''0未发布 1已发布'' AFTER status');
CALL add_col('exam_paper', 'published_at',
             'published_at DATETIME NULL COMMENT ''最近一次发布时间'' AFTER publish_status');
CALL add_col('exam_paper', 'version',
             'version INT NOT NULL DEFAULT 1 COMMENT ''发布版本号，每次重新发布 +1'' AFTER published_at');

-- 存量试卷视为「已发布」，避免升级后学生看不到原来的测验
UPDATE exam_paper SET publish_status = 1, version = 1 WHERE publish_status = 0 OR publish_status IS NULL;
UPDATE exam_paper SET published_at = create_time WHERE published_at IS NULL;

CALL add_col('exam_record', 'paper_version',
             'paper_version INT NOT NULL DEFAULT 1 COMMENT ''作答时所对应试卷的发布版本'' AFTER paper_id');
UPDATE exam_record SET paper_version = 1 WHERE paper_version IS NULL OR paper_version = 0;

-- ---------------- 4. 作业挂章节 ----------------
CALL add_col('assignment', 'chapter_id',
             'chapter_id BIGINT NULL COMMENT ''所属章节，空表示整门课'' AFTER course_id');

DROP PROCEDURE IF EXISTS add_col;
DROP PROCEDURE IF EXISTS add_idx;

SELECT 'v5 upgrade done' AS msg;
