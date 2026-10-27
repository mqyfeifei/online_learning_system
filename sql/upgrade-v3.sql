-- ============================================================
-- v3 升级脚本：课件在线预览 + 主观题批阅 + 选课审核 + 批量导入导出
-- 适用：已经在跑的 online_learning 库（不删库，可重复执行）
-- 内容：
--   1. course 增加「选课是否需审核 / 资料是否可下载 / 导出范围」三个开关
--   2. question 支持填空题(4)与问答题(5)，answer 放宽为 TEXT（问答题存参考答案，可为空）
--   3. exam_record 增加客观分/主观分/批阅状态，新增 exam_mark 主观题批阅明细表
--   4. enrollment.status 语义扩展：0 退课 / 1 在读 / 2 待审核 / 3 已拒绝
-- 执行方式：mysql -u root -p < upgrade-v3.sql
-- ============================================================

USE online_learning;

-- ---------------- 1. 课程设置开关 ----------------
-- MySQL 8+ 不支持 ADD COLUMN IF NOT EXISTS，用存储过程做幂等判断
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

CALL add_col('course', 'enroll_audit',
             'enroll_audit TINYINT NOT NULL DEFAULT 0 COMMENT ''选课是否需要审核 1需要 0不需要'' AFTER status');
CALL add_col('course', 'resource_downloadable',
             'resource_downloadable TINYINT NOT NULL DEFAULT 1 COMMENT ''课件资料是否允许下载 1允许 0仅在线查看'' AFTER enroll_audit');
CALL add_col('course', 'export_scope',
             'export_scope VARCHAR(16) NOT NULL DEFAULT ''ENROLLED'' COMMENT ''资料导出范围 NONE/ENROLLED/ALL'' AFTER resource_downloadable');

-- ---------------- 2. 题目扩展 ----------------
CALL add_col('question', 'reference_answer',
             'reference_answer TEXT COMMENT ''问答题参考答案（无标准答案，供教师批阅参考）'' AFTER answer');
ALTER TABLE question MODIFY COLUMN answer TEXT COMMENT '标准答案；填空多个空用 | 分隔、同空多种写法用 ~ 分隔；问答题可空';

-- ---------------- 3. 考试记录：主观题批阅 ----------------
CALL add_col('exam_record', 'objective_score',
             'objective_score INT DEFAULT 0 COMMENT ''客观题自动判分得分'' AFTER score');
CALL add_col('exam_record', 'subjective_score',
             'subjective_score INT DEFAULT 0 COMMENT ''主观题教师给分'' AFTER objective_score');
CALL add_col('exam_record', 'mark_status',
             'mark_status TINYINT NOT NULL DEFAULT 0 COMMENT ''0无需批阅 1待批阅 2已批阅'' AFTER subjective_score');
CALL add_col('exam_record', 'mark_time',
             'mark_time DATETIME COMMENT '''' AFTER mark_status');
CALL add_col('exam_record', 'marker_id',
             'marker_id BIGINT COMMENT ''批阅教师'' AFTER mark_time');

DROP PROCEDURE IF EXISTS add_col;

-- 主观题批阅明细（一题一行，便于留痕与二次修改）
CREATE TABLE IF NOT EXISTS exam_mark (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    record_id   BIGINT   NOT NULL COMMENT '答卷记录',
    question_id BIGINT   NOT NULL COMMENT '题目',
    score       INT      NOT NULL DEFAULT 0 COMMENT '本题得分',
    comment     VARCHAR(500) COMMENT '批阅评语',
    teacher_id  BIGINT   COMMENT '批阅教师',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_record_question (record_id, question_id),
    KEY idx_record (record_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '主观题批阅明细';

-- ---------------- 4. 选课状态语义扩展 ----------------
ALTER TABLE enrollment MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
    COMMENT '0退课 1在读 2待审核 3已拒绝';

-- 历史数据兜底：已有记录都是「在读」
UPDATE enrollment SET status = 1 WHERE status IS NULL OR status NOT IN (0, 1, 2, 3);

-- ---------------- 5. 演示数据：给已有课程打开开关，插入一道填空、一道问答 ----------------
UPDATE course SET enroll_audit = 0, resource_downloadable = 1, export_scope = 'ENROLLED'
WHERE enroll_audit IS NULL OR export_scope IS NULL;

INSERT INTO question (course_id, type, title, options_json, answer, reference_answer, analysis, difficulty)
SELECT 1, 4, 'Java 中用于定义常量的关键字是 ______。', NULL, 'final', NULL,
       'final 修饰的变量只能赋值一次，习惯上配合 static 作为常量。', 1
WHERE NOT EXISTS(SELECT 1 FROM question WHERE course_id = 1 AND type = 4);

INSERT INTO question (course_id, type, title, options_json, answer, reference_answer, analysis, difficulty)
SELECT 1, 5, '简述 Java 中接口与抽象类的主要区别，并各举一个适用场景。', NULL, NULL,
       '1) 抽象类可以有构造方法与成员变量，接口以常量与抽象方法为主；2) 一个类只能继承一个抽象类，但可实现多个接口；3) 抽象类表达 is-a 关系，接口表达 has-a / 能力约定。场景：模板化流程用抽象类，跨类型能力（如 Comparable）用接口。',
       '问答题不设标准答案，教师按要点给分。', 3
WHERE NOT EXISTS(SELECT 1 FROM question WHERE course_id = 1 AND type = 5);
