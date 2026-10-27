-- ============================================================
-- v2 升级脚本：学习增强能力 + 真实课程数据导入
-- 适用：已经在跑的 online_learning 库（不删库，可重复执行）
-- 内容：
--   1. 新增 favorite / study_note 两张表
--   2. 导入 4 门真实课程（计算机网络 / 数据结构 / 操作系统 / 数据库系统综合实验）
--   3. 课件资源指向 ../upload/course 下已复制好的文件
--   4. 配套作业、题库、试卷与演示学习记录
-- ============================================================

USE online_learning;

-- ---------------- 1. 新表 ----------------
CREATE TABLE IF NOT EXISTS favorite (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    course_id   BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_course (user_id, course_id),
    KEY idx_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '课程收藏';

CREATE TABLE IF NOT EXISTS study_note (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    course_id   BIGINT       NOT NULL,
    chapter_id  BIGINT                COMMENT '可空，挂在章节上时填写',
    title       VARCHAR(120) NOT NULL DEFAULT '' COMMENT '标题，留空时取正文前 20 字',
    content     TEXT         NOT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_course (user_id, course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '学习笔记';

-- ---------------- 2. 清理旧的种子（保证脚本可重复执行） ----------------
DELETE FROM paper_question WHERE paper_id IN (2, 3, 4, 5);
DELETE FROM exam_record WHERE paper_id IN (2, 3, 4, 5);
DELETE FROM exam_paper WHERE id IN (2, 3, 4, 5);
DELETE FROM question WHERE course_id IN (5, 6, 7, 8);
DELETE FROM submission WHERE assignment_id IN (3, 4, 5, 6, 7, 8, 9, 10);
DELETE FROM assignment WHERE id IN (3, 4, 5, 6, 7, 8, 9, 10);
DELETE FROM study_progress WHERE course_id IN (5, 6);
DELETE FROM enrollment WHERE course_id IN (5, 6);
DELETE FROM course_resource WHERE id <= 40;
DELETE FROM chapter WHERE course_id IN (5, 6, 7, 8);
DELETE FROM course WHERE id IN (5, 6, 7, 8);
DELETE FROM course_category WHERE id IN (7, 8, 9);
DELETE FROM favorite WHERE id <= 10;
DELETE FROM study_note WHERE id <= 10;

-- ---------------- 3. 分类 ----------------
INSERT INTO course_category (id, name, parent_id, sort) VALUES
(7, '计算机网络', 0, 5),
(8, '数据结构与算法', 0, 6),
(9, '操作系统', 0, 7);

-- ---------------- 4. 课程 ----------------
INSERT INTO course (id, teacher_id, category_id, title, cover, description, status, view_count, create_time) VALUES
(5, 2, 7, '计算机网络', NULL,
 '从 OSI 与 TCP/IP 分层模型出发，逐层拆解物理层、数据链路层、网络层、运输层和应用层。课程配套谢希仁第 8 版全套课件，重点讲清「数据从一台主机到另一台主机到底经过了什么」。',
 2, 1876, DATE_SUB(NOW(), INTERVAL 40 DAY)),
(6, 2, 8, '数据结构', NULL,
 '线性表、栈与队列、串、树与二叉树、图、查找、排序八大模块，配套讲义 PDF。强调「结构决定算法复杂度」这条主线，每章都有可运行的伪代码与复杂度分析。',
 2, 1532, DATE_SUB(NOW(), INTERVAL 35 DAY)),
(7, 3, 9, '操作系统', NULL,
 '进程与线程、处理机调度、同步与互斥、内存管理、文件系统与设备管理。以实验驱动：从进程调度模拟到页面置换算法，每个实验都有规范化的报告模板与评分要点。',
 2, 964, DATE_SUB(NOW(), INTERVAL 28 DAY)),
(8, 3, 2, '数据库系统综合实验', NULL,
 '围绕一个真实业务场景走完数据库项目的全流程：需求分析 → 概念设计 → 逻辑设计 → 数据库实施 → 数据访问 → 应用系统集成，配套五份实验指导书。',
 2, 1128, DATE_SUB(NOW(), INTERVAL 21 DAY));

-- ---------------- 5. 章节 ----------------
INSERT INTO chapter (id, course_id, title, sort, content) VALUES
-- 计算机网络 9 章
(8, 5, '第 1 章 计算机网络概述', 1, '计算机网络的定义、组成与分类；电路交换、报文交换与分组交换的对比；计算机网络的性能指标体系。本节对应课件《chap1 概述》。'),
(9, 5, '第 2 章 网络性能指标', 2, '速率与带宽、吞吐量、时延（发送/传播/处理/排队）、时延带宽积、往返时间 RTT 与利用率。做题时注意区分「带宽」和「吞吐量」。'),
(10, 5, '第 3 章 网络协议体系结构', 3, '协议三要素（语法、语义、同步）；OSI 七层参考模型与 TCP/IP 四层模型的对应关系；各层PDU 与典型协议。'),
(11, 5, '第 4 章 物理层（上）：数据通信基础', 4, '数据通信系统模型、信道的极限容量（奈氏准则、香农公式）、常用的编码与调制方式。'),
(12, 5, '第 5 章 物理层（下）：传输介质与复用', 5, '双绞线、同轴电缆、光纤与无线传输介质；频分、时分、波分与码分复用；ADSL 与光纤接入。'),
(13, 5, '第 6 章 数据链路层', 6, '差错检测（CRC）、点对点协议 PPP、CSMA/CD 与以太网帧格式；交换机自学习与 VLAN 划分是本实验课的重点。'),
(14, 5, '第 7 章 网络层', 7, 'IPv4 地址与子网划分、CIDR 与路由聚合、ARP 与 ICMP、距离向量与链路状态路由算法。'),
(15, 5, '第 8 章 运输层', 8, 'UDP 与 TCP 的对比；TCP 三次握手与四次挥手、滑动窗口流量控制、慢开始与拥塞避免。'),
(16, 5, '第 9 章 应用层', 9, 'HTTP 报文与状态码、DNS 递归与迭代查询、SMTP/POP3 与 FTP 的工作过程。'),
-- 数据结构 8 章
(17, 6, '第 1 章 绪论', 1, '数据结构的基本概念、抽象数据类型 ADT、算法的时间复杂度与空间复杂度分析（大 O 记号的推导方法）。'),
(18, 6, '第 2 章 线性表', 2, '顺序表与链表的存储结构对比；单链表、双向链表、循环链表的插入删除；头插法与尾插法的差异。'),
(19, 6, '第 3 章 栈和队列', 3, '栈的后进先出特性与应用（括号匹配、表达式求值、递归）；队列、循环队列与双端队列；顺序队与链队的实现。'),
(20, 6, '第 4 章 串', 4, '串的存储结构；朴素模式匹配与 KMP 算法；next 数组与 nextval 数组的推导过程。'),
(21, 6, '第 5 章 树与二叉树', 5, '二叉树的五种基本形态与性质；先中后序遍历（递归与非递归）；线索二叉树；哈夫曼树与哈夫曼编码。'),
(22, 6, '第 6 章 图', 6, '图的邻接矩阵与邻接表存储；深度优先与广度优先遍历；Prim、Kruskal 最小生成树；Dijkstra 最短路径与拓扑排序。'),
(23, 6, '第 7 章 查找', 7, '顺序查找、折半查找、分块查找；二叉排序树与平衡二叉树（AVL 的四种旋转）；B 树与 B+ 树；散列表与冲突处理。'),
(24, 6, '第 8 章 排序', 8, '插入排序、交换排序（冒泡、快排）、选择排序（简单选择、堆排序）、归并排序与基数排序；稳定性与复杂度对比总结。'),
-- 操作系统 5 章
(25, 7, '第 1 讲 课程导学与课前提问', 1, '课程目标、考核方式与实验安排；课前思考题清单，建议先自己写一遍答案再对照讲义。'),
(26, 7, '第 2 讲 第三章作业讲解', 2, '进程同步与互斥的经典问题：生产者—消费者、读者—写者、哲学家进餐；信号量 P/V 操作的规范写法。'),
(27, 7, '第 3 讲 实验报告书写规范', 3, '实验报告的统一格式要求：实验目的、原理、步骤、结果分析、问题与思考。评审时会按模板逐项打分。'),
(28, 7, '第 4 讲 实验一：进程调度算法模拟', 4, '实现 FCFS、SJF、时间片轮转三种调度算法，输出平均周转时间与平均带权周转时间，并对比分析。'),
(29, 7, '第 5 讲 实验二：内存管理与页面置换', 5, '模拟 FIFO、LRU、OPT 三种页面置换算法，统计缺页率并绘制缺页率随物理块数变化的曲线。'),
-- 数据库系统综合实验 6 讲
(30, 8, '实验总览：从需求到应用系统', 1, '五份实验指导书的关系梳理，项目选题要求、时间节点与提交物清单。'),
(31, 8, '实验一 需求分析', 2, '业务调研、数据流图与数据字典的绘制；用 E-R 图表达实体、属性与联系（1:1、1:n、m:n）。'),
(32, 8, '实验二 数据库设计', 3, '概念结构设计到逻辑结构设计；E-R 图向关系模式的转换规则；范式判定与反规范化取舍。'),
(33, 8, '实验三 数据库实施', 4, '建库建表、约束与索引设计、视图与存储过程；用测试数据验证完整性约束是否生效。'),
(34, 8, '实验四 数据访问', 5, 'JDBC / MyBatis 访问数据库；连接池配置；事务边界与隔离级别的实际影响。'),
(35, 8, '实验五 应用系统', 6, '把数据库接入应用系统，完成增删改查与报表；部署与性能调优的基本手段。');

-- ---------------- 6. 课件资源 ----------------
INSERT INTO course_resource (id, course_id, chapter_id, name, file_url, file_type, file_size) VALUES
-- 计算机网络
(1, 5, 8,  'chap1 概述.pptx',            '/files/course/network/01-概述.pptx', 'pptx', 5115068),
(2, 5, 9,  '网络性能指标.pptx',           '/files/course/network/02-网络性能指标.pptx', 'pptx', 995530),
(3, 5, 10, '网络协议体系结构.pptx',        '/files/course/network/03-网络协议体系结构.pptx', 'pptx', 8341351),
(4, 5, 11, '物理层 2.1-2.2.pptx',        '/files/course/network/04-物理层-2.1-2.2.pptx', 'pptx', 2416389),
(5, 5, 12, '物理层 2.3-2.4.pptx',        '/files/course/network/05-物理层-2.3-2.4.pptx', 'pptx', 4945776),
(6, 5, 13, '第 3 章 数据链路层.pptx',      '/files/course/network/06-数据链路层.pptx', 'pptx', 4871268),
(7, 5, 14, '第 4 章 网络层.pptx',          '/files/course/network/07-网络层.pptx', 'pptx', 6721760),
(8, 5, 15, '第 5 章 运输层.pptx',          '/files/course/network/08-运输层.pptx', 'pptx', 5628921),
(9, 5, 16, '第 6 章 应用层.pptx',          '/files/course/network/09-应用层.pptx', 'pptx', 5841888),
-- 数据结构
(10, 6, 17, '第 1 章 绪论.pdf',            '/files/course/ds/01-绪论.pdf', 'pdf', 4488335),
(11, 6, 18, '第 2 章 线性表.pdf',          '/files/course/ds/02-线性表.pdf', 'pdf', 7743812),
(12, 6, 19, '第 3 章 栈和队列.pdf',        '/files/course/ds/03-栈和队列.pdf', 'pdf', 6648453),
(13, 6, 20, '第 4 章 串.pdf',              '/files/course/ds/04-串.pdf', 'pdf', 5441182),
(14, 6, 21, '第 5 章 树与二叉树.pdf',      '/files/course/ds/05-树与二叉树.pdf', 'pdf', 15158805),
(15, 6, 22, '第 6 章 图.pdf',              '/files/course/ds/06-图.pdf', 'pdf', 13780268),
(16, 6, 23, '第 7 章 查找.pdf',            '/files/course/ds/07-查找.pdf', 'pdf', 22662549),
(17, 6, 24, '第 8 章 排序.pdf',            '/files/course/ds/08-排序.pdf', 'pdf', 16320530),
-- 操作系统
(18, 7, 25, '操作系统课前提问.md',          '/files/course/os/01-课前提问.md', 'md', 18670),
(19, 7, 26, '第三章作业.pdf',              '/files/course/os/02-第三章作业.pdf', 'pdf', 75808),
(20, 7, 27, '操作系统实验报告书模板.docx',   '/files/course/os/03-实验报告书模板.docx', 'docx', 879005),
(21, 7, 28, '实验报告模板（一）.docx',       '/files/course/os/04-实验报告模板一.docx', 'docx', 1737272),
(22, 7, 29, '实验报告模板（二）.docx',       '/files/course/os/05-实验报告模板二.docx', 'docx', 1855351),
-- 数据库
(23, 8, 30, '数据库系统综合实验指导书.docx', '/files/course/db/01-实验指导书.docx', 'docx', 139725),
(24, 8, 31, '实验一 需求分析.docx',          '/files/course/db/02-实验一-需求分析.docx', 'docx', 52097),
(25, 8, 32, '实验二 数据库设计.docx',        '/files/course/db/03-实验二-数据库设计.docx', 'docx', 83024),
(26, 8, 33, '实验三 数据库实施.docx',        '/files/course/db/04-实验三-数据库实施.docx', 'docx', 26955),
(27, 8, 34, '实验四 数据访问.pptx',          '/files/course/db/05-实验四-数据访问.pptx', 'pptx', 169621),
(28, 8, 35, '实验五 应用系统.docx',          '/files/course/db/06-实验五-应用系统.docx', 'docx', 49733);

-- ---------------- 7. 作业 ----------------
INSERT INTO assignment (id, course_id, title, requirement, deadline, create_time) VALUES
(3, 5, '作业一：子网划分与 CIDR 计算',
 '给定 192.168.10.0/24，要求划分为 6 个子网，写出每个子网的网络地址、广播地址与可用主机范围，并说明如何用 CIDR 做路由聚合。提交 PDF 或手写拍照。',
 DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 5, '作业二：抓包分析 TCP 三次握手',
 '用 Wireshark 抓取一次 HTTP 访问的完整报文，截出 SYN / SYN+ACK / ACK 三个包，标注序号、确认号与标志位含义，并解释为什么需要三次而不是两次。',
 DATE_ADD(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 6, '作业一：单链表逆置与复杂度分析',
 '实现单链表的原地逆置（不允许申请新结点），给出代码与时间复杂度推导；再对比头插法重建链表的空间开销。',
 DATE_ADD(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(6, 6, '作业二：哈夫曼编码构造',
 '给定字符集及权值 {A:5, B:9, C:12, D:13, E:16, F:45}，构造哈夫曼树，写出每个字符的编码与加权路径长度 WPL。',
 DATE_ADD(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 7, '作业一：生产者—消费者问题的信号量实现',
 '用 P/V 操作写出有界缓冲区的生产者—消费者模型，要求缓冲区大小为 N，说明每个信号量的初值与含义，并分析是否会出现死锁。',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(8, 7, '作业二：页面置换算法对比',
 '对访问串 7,0,1,2,0,3,0,4,2,3,0,3,2,1,2,0,1,7,0,1，分别用 FIFO、LRU、OPT 在 3 个物理块下模拟，列出缺页次数并画缺页率曲线。',
 DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9, 8, '实验一：需求分析与 E-R 图',
 '自选一个业务场景（图书借阅、教务管理、电商订单等），完成需求调研记录、数据流图与 E-R 图，标注实体属性与联系类型。',
 DATE_ADD(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(10, 8, '实验二：关系模式转换与范式判定',
 '把实验一的 E-R 图转换为关系模式，标出主键与外键，逐个判断是否满足 3NF，不满足的给出分解过程。',
 DATE_ADD(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------- 8. 题库 ----------------
INSERT INTO question (id, course_id, type, title, options_json, answer, analysis, difficulty) VALUES
-- 计算机网络
(11, 5, 1, 'OSI 参考模型中，负责端到端可靠传输的是哪一层？',
 '[{"key":"A","text":"数据链路层"},{"key":"B","text":"网络层"},{"key":"C","text":"运输层"},{"key":"D","text":"会话层"}]', 'C',
 '运输层（TCP）提供端到端的可靠传输；数据链路层只管相邻结点之间，网络层负责主机到主机的路由。', 1),
(12, 5, 1, '在 TCP/IP 体系中，IP 协议工作在哪一层？',
 '[{"key":"A","text":"网络接口层"},{"key":"B","text":"网际层"},{"key":"C","text":"运输层"},{"key":"D","text":"应用层"}]', 'B',
 'IP 属于网际层（对应 OSI 的网络层），负责无连接的数据报投递。', 1),
(13, 5, 1, '一个 192.168.1.0/26 的子网最多能容纳多少台主机？',
 '[{"key":"A","text":"62"},{"key":"B","text":"64"},{"key":"C","text":"30"},{"key":"D","text":"126"}]', 'A',
 '/26 的主机位是 6 位，2^6 - 2 = 62，要扣掉网络地址和广播地址。', 2),
(14, 5, 1, 'TCP 建立连接时，第二次握手发送的报文段标志位是？',
 '[{"key":"A","text":"SYN=1, ACK=0"},{"key":"B","text":"SYN=1, ACK=1"},{"key":"C","text":"SYN=0, ACK=1"},{"key":"D","text":"FIN=1, ACK=1"}]', 'B',
 '服务器收到 SYN 后回复 SYN+ACK，既确认对方的 SYN，又发出自己的同步请求。', 2),
(15, 5, 2, '下列属于数据链路层功能的有？',
 '[{"key":"A","text":"差错检测"},{"key":"B","text":"流量控制"},{"key":"C","text":"路由选择"},{"key":"D","text":"介质访问控制"}]', 'A,B,D',
 '路由选择是网络层的职责，其余三项都属于数据链路层。', 2),
(16, 5, 2, '关于 TCP 与 UDP，说法正确的有？',
 '[{"key":"A","text":"TCP 面向连接，UDP 无连接"},{"key":"B","text":"UDP 首部固定 8 字节"},{"key":"C","text":"TCP 提供流量控制"},{"key":"D","text":"UDP 保证数据按序到达"}]', 'A,B,C',
 'UDP 不保证顺序也不保证可靠，D 错误。', 2),
(17, 5, 3, 'CSMA/CD 只适用于半双工的总线型以太网。', NULL, 'T',
 '全双工交换式以太网不存在冲突，不需要冲突检测，所以 CSMA/CD 已不再使用。', 2),
(18, 5, 3, '同一局域网内，两台主机 IP 地址相同但 MAC 地址不同仍可正常通信。', NULL, 'F',
 'IP 地址冲突会导致 ARP 应答混乱，通信必然异常。', 1),
-- 数据结构
(19, 6, 1, '在长度为 n 的顺序表第 i 个位置插入元素，平均需要移动多少个元素？',
 '[{"key":"A","text":"n"},{"key":"B","text":"n/2"},{"key":"C","text":"(n-1)/2"},{"key":"D","text":"1"}]', 'B',
 '插入位置等概率时平均移动 n/2 个元素，删除同理是 (n-1)/2。', 2),
(20, 6, 1, '下列排序算法中，平均时间复杂度不是 O(n log n) 的是？',
 '[{"key":"A","text":"快速排序"},{"key":"B","text":"归并排序"},{"key":"C","text":"堆排序"},{"key":"D","text":"冒泡排序"}]', 'D',
 '冒泡排序平均与最坏都是 O(n²)，其余三个平均为 O(n log n)。', 1),
(21, 6, 1, '具有 n 个结点的完全二叉树，其深度（根为第 1 层）为？',
 '[{"key":"A","text":"⌊log₂n⌋ + 1"},{"key":"B","text":"⌈log₂n⌉"},{"key":"C","text":"n/2"},{"key":"D","text":"2n - 1"}]', 'A',
 '完全二叉树深度为 ⌊log₂n⌋ + 1，也可以用 ⌈log₂(n+1)⌉ 表示。', 2),
(22, 6, 2, '下列哪些排序算法是稳定的？',
 '[{"key":"A","text":"冒泡排序"},{"key":"B","text":"归并排序"},{"key":"C","text":"快速排序"},{"key":"D","text":"插入排序"}]', 'A,B,D',
 '快速排序在交换过程中会打乱相同元素的相对次序，不稳定。', 2),
(23, 6, 3, 'KMP 算法的时间复杂度是 O(n+m)，其中 n 为主串长度、m 为模式串长度。', NULL, 'T',
 'next 数组预处理 O(m)，匹配过程主串指针不回退，总共 O(n+m)。', 2),
-- 操作系统
(24, 7, 1, '产生死锁的四个必要条件中，不包括下列哪一项？',
 '[{"key":"A","text":"互斥条件"},{"key":"B","text":"请求并保持"},{"key":"C","text":"不可剥夺"},{"key":"D","text":"优先级反转"}]', 'D',
 '四个必要条件是互斥、请求并保持、不可剥夺、循环等待；优先级反转是调度问题，不是死锁条件。', 2),
(25, 7, 1, '页面置换算法中，理论上缺页率最低的是？',
 '[{"key":"A","text":"FIFO"},{"key":"B","text":"LRU"},{"key":"C","text":"OPT"},{"key":"D","text":"CLOCK"}]', 'C',
 'OPT（最佳置换）置换未来最久不用的页面，缺页率理论最低，但无法实际实现，常作为评价基准。', 1),
(26, 7, 2, '下列属于进程与线程区别的有？',
 '[{"key":"A","text":"进程是资源分配单位，线程是调度单位"},{"key":"B","text":"同一进程的线程共享地址空间"},{"key":"C","text":"线程切换开销小于进程"},{"key":"D","text":"线程拥有独立的页表"}]', 'A,B,C',
 '同一进程内线程共享页表与地址空间，D 错误。', 2),
(27, 7, 3, '信号量的 P 操作会使信号量减 1，若结果小于 0 则进程进入等待队列。', NULL, 'T',
 '这正是 P/V 操作实现同步与互斥的基本机制。', 1),
-- 数据库
(28, 8, 1, 'E-R 图中表示实体之间多对多联系，转换后通常会产生什么？',
 '[{"key":"A","text":"一个新的关系模式"},{"key":"B","text":"一个外键"},{"key":"C","text":"一个触发器"},{"key":"D","text":"一个存储过程"}]', 'A',
 'm:n 联系必须单独建一张联系表，主键由两端实体的主键组合而成。', 2),
(29, 8, 1, '下列哪一项不是事务的 ACID 特性？',
 '[{"key":"A","text":"原子性"},{"key":"B","text":"一致性"},{"key":"C","text":"隔离性"},{"key":"D","text":"可扩展性"}]', 'D',
 'ACID 是原子性、一致性、隔离性、持久性，可扩展性不属于其中。', 1),
(30, 8, 2, '关于 3NF，下列说法正确的有？',
 '[{"key":"A","text":"满足 2NF"},{"key":"B","text":"消除非主属性对主键的传递依赖"},{"key":"C","text":"一定满足 BCNF"},{"key":"D","text":"允许存在部分函数依赖"}]', 'A,B',
 '3NF 是在 2NF 基础上消除传递依赖；3NF 不一定满足 BCNF，也不允许部分依赖。', 2),
(31, 8, 3, '在 MySQL 中，InnoDB 的默认事务隔离级别是 REPEATABLE READ。', NULL, 'T',
 'InnoDB 默认 REPEATABLE READ，并通过 MVCC + Next-Key Lock 在一定程度上避免幻读。', 2);

-- ---------------- 9. 试卷 ----------------
INSERT INTO exam_paper (id, course_id, title, total_score, pass_score, duration, status, create_time) VALUES
(2, 5, '计算机网络分层与协议测验', 100, 60, 20, 1, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, 6, '数据结构基础测验',       100, 60, 20, 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(4, 7, '操作系统进程与内存测验', 100, 60, 15, 1, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(5, 8, '数据库设计与范式测验',   100, 60, 15, 1, DATE_SUB(NOW(), INTERVAL 7 DAY));

INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT 2, id, 12, id FROM question WHERE course_id = 5;
INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT 3, id, 20, id FROM question WHERE course_id = 6;
INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT 4, id, 25, id FROM question WHERE course_id = 7;
INSERT INTO paper_question (paper_id, question_id, score, sort)
SELECT 5, id, 25, id FROM question WHERE course_id = 8;

-- ---------------- 10. 演示学习数据（student1 = id 4） ----------------
INSERT INTO enrollment (user_id, course_id, status, create_time) VALUES
(4, 5, 1, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(4, 6, 1, DATE_SUB(NOW(), INTERVAL 9 DAY));

-- 计算机网络学完 4 章、数据结构学完 3 章，时间打散到最近 7 天，让周活跃图有起伏
INSERT INTO study_progress (user_id, course_id, chapter_id, finished, finish_time) VALUES
(4, 5, 8,  1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 5, 9,  1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 5, 10, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 5, 11, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 6, 17, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 6, 18, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 6, 19, 1, NOW());

-- 一次测验记录 + 一次作业批阅，用于成绩趋势图
INSERT INTO exam_record (paper_id, user_id, answers_json, score, passed, start_time, submit_time) VALUES
(2, 4, '{"11":"C","12":"B","13":"A","14":"B","15":"A,B,D","16":"A,B,C","17":"T","18":"F"}', 84, 1,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO submission (assignment_id, user_id, content, score, `comment`, status, submit_time) VALUES
(3, 4, '6 个子网分别为 192.168.10.0/27、.32/27、.64/27、.96/27、.128/27、.160/27，可用主机范围已列表说明。',
 88, '子网划分正确，路由聚合那段可以再补充「最长前缀匹配」的说明。', 1, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ---------------- 11. 收藏与笔记演示数据 ----------------
INSERT INTO favorite (user_id, course_id, create_time) VALUES
(4, 5, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 4, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO study_note (user_id, course_id, chapter_id, title, content, create_time, update_time) VALUES
(4, 5, 9, '时延的四种组成',
 '发送时延 = 数据长度 / 发送速率；传播时延 = 信道长度 / 电磁波传播速率。排队时延和处理时延通常忽略，但在拥塞时占主导。考试常考「带宽增大能否降低传播时延」——不能。',
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 6, 21, '二叉树遍历手写技巧',
 '先序：根左右；中序：左根右；后序：左右根。非递归用栈实现，中序最容易写错——先一路压左孩子，弹出时访问再转向右子树。',
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------- 12. 公告 ----------------
INSERT INTO sys_notice (title, content, top_flag, status, create_time) VALUES
('四门专业必修课已上线', '《计算机网络》《数据结构》《操作系统》《数据库系统综合实验》四门课程已上架，配套课件可直接在课程内下载。', 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
('期中测验安排', '计算机网络、数据结构测验已开放，请在「成绩与测验」中查看，限时 20 分钟，可重考一次。', 0, 1, DATE_SUB(NOW(), INTERVAL 2 DAY));
