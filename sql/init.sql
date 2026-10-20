-- ============================================================
-- 在线学习平台 数据库初始化脚本
-- 适用：MySQL 9.2（排序规则用 utf8mb4_0900_ai_ci，别用 8.0 时代的 general_ci）
-- 执行方式：mysql -u root -p < init.sql  或 Navicat 里整段运行
-- 初始账号：admin / teacher1 / student1，密码统一 123456
-- ============================================================

DROP DATABASE IF EXISTS online_learning;
CREATE DATABASE online_learning
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE online_learning;

-- ---------------- 1. 用户表 ----------------
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名',
    password    VARCHAR(64)  NOT NULL COMMENT 'MD5(明文+salt)',
    salt        VARCHAR(32)  NOT NULL COMMENT '盐',
    real_name   VARCHAR(50)           COMMENT '真实姓名',
    avatar      VARCHAR(255)          COMMENT '头像地址',
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT/TEACHER/ADMIN',
    phone       VARCHAR(20)           COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户表';

-- ---------------- 2. 课程分类 ----------------
CREATE TABLE course_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '分类名',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类，0为一级',
    sort        INT         NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '课程分类';

-- ---------------- 3. 课程 ----------------
CREATE TABLE course (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    teacher_id    BIGINT       NOT NULL COMMENT '授课教师',
    category_id   BIGINT                COMMENT '分类',
    title         VARCHAR(100) NOT NULL,
    cover         VARCHAR(255)          COMMENT '封面图',
    description   TEXT                  COMMENT '课程简介',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1待审 2已发布 3下架 4驳回',
    reject_reason VARCHAR(255)          COMMENT '驳回理由',
    view_count    INT          NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_teacher (teacher_id),
    KEY idx_category (category_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '课程表';

-- ---------------- 4. 章节 ----------------
CREATE TABLE chapter (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    course_id   BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    video_url   VARCHAR(255)          COMMENT '视频地址',
    content     LONGTEXT              COMMENT '图文内容',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '章节表';

-- ---------------- 5. 课件资源 ----------------
CREATE TABLE course_resource (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    course_id   BIGINT       NOT NULL,
    chapter_id  BIGINT                COMMENT '可空，表示课程级资源',
    name        VARCHAR(150) NOT NULL,
    file_url    VARCHAR(255) NOT NULL,
    file_type   VARCHAR(20)           COMMENT 'pdf/ppt/mp4...',
    file_size   BIGINT                COMMENT '字节',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '课件资源';

-- ---------------- 6. 选课记录 ----------------
CREATE TABLE enrollment (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    course_id   BIGINT   NOT NULL,
    status      TINYINT  NOT NULL DEFAULT 1 COMMENT '1在读 0退课',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_course (user_id, course_id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '选课记录';

-- ---------------- 7. 学习进度 ----------------
CREATE TABLE study_progress (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    course_id   BIGINT   NOT NULL,
    chapter_id  BIGINT   NOT NULL,
    finished    TINYINT  NOT NULL DEFAULT 1 COMMENT '1已学完',
    finish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_chapter (user_id, chapter_id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '学习进度';

-- ---------------- 8. 作业 ----------------
CREATE TABLE assignment (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    course_id   BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    requirement TEXT                  COMMENT '作业要求',
    attach_url  VARCHAR(255)          COMMENT '附件模板',
    deadline    DATETIME              COMMENT '截止时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '作业表';

-- ---------------- 9. 作业提交 ----------------
CREATE TABLE submission (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    content       TEXT                  COMMENT '文字说明',
    attach_url    VARCHAR(255)          COMMENT '附件',
    score         INT                   COMMENT '分数',
    `comment`     VARCHAR(500)          COMMENT '教师评语',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待批阅 1已批阅',
    submit_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_assignment_user (assignment_id, user_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '作业提交';

-- ---------------- 10. 题库 ----------------
CREATE TABLE question (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    course_id    BIGINT       NOT NULL,
    type         TINYINT      NOT NULL COMMENT '1单选 2多选 3判断',
    title        VARCHAR(500) NOT NULL,
    options_json TEXT                  COMMENT '[{"key":"A","text":"..."}]',
    answer       VARCHAR(50)  NOT NULL COMMENT '多选逗号分隔，判断T/F',
    analysis     VARCHAR(1000)         COMMENT '解析',
    difficulty   TINYINT      NOT NULL DEFAULT 1 COMMENT '1简单 2中等 3困难',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '题库';

-- ---------------- 11. 试卷 ----------------
CREATE TABLE exam_paper (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    course_id   BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    total_score INT          NOT NULL DEFAULT 100,
    pass_score  INT          NOT NULL DEFAULT 60,
    duration    INT          NOT NULL DEFAULT 30 COMMENT '分钟',
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '试卷';

-- ---------------- 12. 试卷-题目 ----------------
CREATE TABLE paper_question (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    paper_id    BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score       INT    NOT NULL DEFAULT 10,
    sort        INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_paper_question (paper_id, question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '试卷题目关联';

-- ---------------- 13. 考试记录 ----------------
CREATE TABLE exam_record (
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    paper_id     BIGINT   NOT NULL,
    user_id      BIGINT   NOT NULL,
    answers_json TEXT              COMMENT '{"题目ID":"A,C"}',
    score        INT               COMMENT '得分',
    passed       TINYINT           COMMENT '1及格 0不及格',
    start_time   DATETIME          COMMENT '开始答题时间',
    submit_time  DATETIME          COMMENT '交卷时间',
    PRIMARY KEY (id),
    KEY idx_user_paper (user_id, paper_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '考试记录';

-- ---------------- 14. 讨论帖 ----------------
CREATE TABLE discussion_post (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    course_id   BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(150) NOT NULL,
    content     TEXT,
    like_count  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0已删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '讨论帖';

-- ---------------- 15. 讨论回复 ----------------
CREATE TABLE discussion_reply (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    post_id     BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    content     TEXT     NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_post (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '讨论回复';

-- ---------------- 16. 系统公告 ----------------
CREATE TABLE sys_notice (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(150) NOT NULL,
    content     TEXT,
    top_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '1置顶',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1显示 0撤下',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '系统公告';

-- ============================================================
-- 初始化数据
-- 密码算法：MD5(明文 + salt)，salt 统一 a1b2c3d4
-- 123456 + a1b2c3d4 = 416a903d7521048c970c8388821b30c6
-- ============================================================
INSERT INTO sys_user (username, password, salt, real_name, role, phone, status) VALUES
('admin',    '416a903d7521048c970c8388821b30c6', 'a1b2c3d4', '系统管理员', 'ADMIN',   '13800000000', 1),
('teacher1', '416a903d7521048c970c8388821b30c6', 'a1b2c3d4', '刘老师',     'TEACHER', '13800000001', 1),
('teacher2', '416a903d7521048c970c8388821b30c6', 'a1b2c3d4', '陈老师',     'TEACHER', '13800000002', 1),
('student1', '416a903d7521048c970c8388821b30c6', 'a1b2c3d4', '莫秋裕',     'STUDENT', '13800000003', 1),
('student2', '416a903d7521048c970c8388821b30c6', 'a1b2c3d4', '李同学',     'STUDENT', '13800000004', 1);

INSERT INTO course_category (name, parent_id, sort) VALUES
('编程语言', 0, 1),
('数据库',   0, 2),
('人工智能', 0, 3),
('前端开发', 0, 4),
('Java',     1, 1),
('Python',   1, 2);

INSERT INTO course (teacher_id, category_id, title, description, status, view_count) VALUES
(2, 5, 'Java 程序设计基础', '从变量、流程控制到面向对象，配套 4 个章节与随堂测验，适合零基础入门。', 2, 320),
(2, 2, '数据库系统原理',   '关系模型、SQL 语法、范式设计与事务并发控制，课程含作业与综合测验。', 2, 256),
(3, 3, 'Python 与数据分析', 'Pandas / NumPy 数据处理与可视化入门，本课程正在等待管理员审核。', 1, 88),
(3, 4, 'Vue3 前端实战',     '组合式 API + Vite + Element Plus 构建后台管理系统的完整实践。', 0, 0);

INSERT INTO chapter (course_id, title, sort, content) VALUES
(1, '第 1 章 Java 开发环境与第一个程序', 1, 'JDK 安装、IDEA 配置、Hello World 的编译与运行过程。'),
(1, '第 2 章 变量与数据类型', 2, '八种基本类型、自动拆装箱、String 常用方法。'),
(1, '第 3 章 流程控制与数组', 3, 'if/switch/for/while 与一维数组、增强 for 循环。'),
(1, '第 4 章 面向对象三大特性', 4, '封装、继承、多态，以及接口与抽象类的区别。'),
(2, '第 1 章 关系模型与范式', 1, '关系、元组、属性；1NF/2NF/3NF 的判定方法。'),
(2, '第 2 章 SQL 基础', 2, 'DDL/DML/DQL 三类语句与常用聚合函数。'),
(2, '第 3 章 事务与并发控制', 3, 'ACID 特性、隔离级别与锁机制。');

INSERT INTO assignment (course_id, title, requirement, deadline) VALUES
(1, '作业一：面向对象练习', '用面向对象思想设计一个图书管理类，要求包含封装、继承与方法重写，提交源码压缩包。', DATE_ADD(NOW(), INTERVAL 7 DAY)),
(2, '作业一：数据库表设计', '根据电商场景画出 ER 图并写出建表 SQL，要求满足第三范式。', DATE_ADD(NOW(), INTERVAL 5 DAY));

INSERT INTO question (course_id, type, title, options_json, answer, analysis, difficulty) VALUES
(1, 1, 'Java 中 int 类型占用多少个字节？', '[{"key":"A","text":"2"},{"key":"B","text":"4"},{"key":"C","text":"8"},{"key":"D","text":"与平台相关"}]', 'B', 'Java 的基本类型长度是固定的，int 固定 4 字节 32 位。', 1),
(1, 1, '下列哪个关键字用于实现继承？', '[{"key":"A","text":"implements"},{"key":"B","text":"extends"},{"key":"C","text":"inherits"},{"key":"D","text":"super"}]', 'B', '类继承用 extends，接口实现用 implements。', 1),
(1, 1, 'String 对象一旦创建，其内容是否可以修改？', '[{"key":"A","text":"可以"},{"key":"B","text":"不可以"},{"key":"C","text":"仅能改长度"},{"key":"D","text":"取决于 JVM"}]', 'B', 'String 是不可变对象，任何“修改”都会产生新对象。', 1),
(1, 1, '以下哪个不是 Java 的访问修饰符？', '[{"key":"A","text":"public"},{"key":"B","text":"private"},{"key":"C","text":"protected"},{"key":"D","text":"friendly"}]', 'D', 'Java 的访问修饰符只有 public、protected、private 和默认（不写），friendly 是 C++ 的说法。', 2),
(1, 1, '接口中定义的方法默认是什么修饰？', '[{"key":"A","text":"public abstract"},{"key":"B","text":"private"},{"key":"C","text":"protected"},{"key":"D","text":"static"}]', 'A', '接口方法默认 public abstract，Java 8 起可写 default 和 static 方法。', 2),
(1, 2, '下列属于面向对象三大特性的有？', '[{"key":"A","text":"封装"},{"key":"B","text":"继承"},{"key":"C","text":"多态"},{"key":"D","text":"重载"}]', 'A,B,C', '三大特性是封装、继承、多态；重载只是方法的一种形式。', 2),
(1, 2, '下列哪些是 Java 的基本数据类型？', '[{"key":"A","text":"int"},{"key":"B","text":"String"},{"key":"C","text":"boolean"},{"key":"D","text":"double"}]', 'A,C,D', 'String 是引用类型（类），不是基本类型。', 2),
(1, 2, '关于集合，下列说法正确的有？', '[{"key":"A","text":"ArrayList 底层是数组"},{"key":"B","text":"HashMap 允许一个 null 键"},{"key":"C","text":"LinkedList 查询比 ArrayList 快"},{"key":"D","text":"HashSet 元素不重复"}]', 'A,B,D', 'LinkedList 插入删除快、随机访问慢，C 错误。', 3),
(1, 3, 'Java 支持类的多重继承。', NULL, 'F', 'Java 类不支持多重继承，但接口可以多实现。', 1),
(1, 3, 'final 修饰的变量不能被重新赋值。', NULL, 'T', 'final 变量只能赋值一次。', 1);

INSERT INTO exam_paper (course_id, title, total_score, pass_score, duration, status) VALUES
(1, 'Java 基础随堂测验', 100, 60, 30, 1);

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT 1, id, 10, id FROM question WHERE course_id = 1;

INSERT INTO sys_notice (title, content, top_flag, status) VALUES
('平台上线试运行通知', '在线学习平台已进入试运行阶段，欢迎同学反馈使用问题。', 1, 1),
('期中教学检查安排', '本学期期中教学检查定于第 9 周进行，请各位教师及时完善课程资料。', 0, 1);

-- 演示数据：student1 已选课程 1，并学完前 2 章
INSERT INTO enrollment (user_id, course_id, status) VALUES (4, 1, 1);
INSERT INTO study_progress (user_id, course_id, chapter_id, finished) VALUES
(4, 1, 1, 1),
(4, 1, 2, 1);

INSERT INTO discussion_post (course_id, user_id, title, content, like_count, status) VALUES
(1, 4, '关于 final 关键字的疑问', 'final 修饰引用类型时，是引用不能变还是对象内容不能变？', 3, 1);

INSERT INTO discussion_reply (post_id, user_id, content) VALUES
(1, 2, '是引用不能重新指向新对象，但对象内部属性仍可修改。');
