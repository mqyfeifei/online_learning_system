# 在线学习平台（Online Learning System）

计算机软件综合开发实训项目 · 基于 Spring Boot 3 + Vue 3 的前后端分离在线学习平台。

学员端（选课 / 学习 / 作业 / 测验 / 成绩 / 收藏笔记 / 讨论区）、教师端（建课 / 章节课件 /
题库 / 试卷发布 / 作业批阅 / 学情）、管理端（统计 / 用户导入 / 课程审核 / 分类 / 公告）三端一体。
界面采用**暖米橙**视觉（底色 `#fdf7f1`、主色 `#ff6b35`），样式集中在 `olearning-web/src/styles/theme.css`。

## 技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.5.16（Java 17） |
| 持久层 | MyBatis-Plus（含 jsqlparser） | 3.5.9 |
| 数据库 | MySQL | 9.2（驱动 mysql-connector-j 8.4.0，排序规则 `utf8mb4_0900_ai_ci`） |
| 认证 | JWT（jjwt） | 0.12.6 |
| 接口文档 | springdoc OpenAPI | 2.8.6 |
| 文档解析 | Apache POI（poi-ooxml + poi-scratchpad） | 5.2.5 |
| 前端框架 | Vue 3 + Vite + TypeScript | Vue 3.5.13 / Vite 6.0.1 / TS 5.6.3 |
| UI 组件 | Element Plus（+ icons-vue） | 2.8.8 |
| 状态与路由 | Pinia / Vue Router | 2.2.6 / 4.4.5 |
| 图表 | ECharts（教师、管理端） | 5.5.1 |
| 请求 | axios | 1.7.7 |
| 构建 | Maven 3.9.11 / Node 22 | — |

未引入 Redis、消息队列与对象存储：缓存与文件均落在单机（本机 Redis 3.0 兼容性风险，
故按开发规划不启用）；文件存 `olearning-server/upload/`，可平滑迁移 MinIO / OSS。

## 目录结构

```
online_learning_system/
├── olearning-server/                后端（Spring Boot，根包 com.mqy.olearning）
│   └── src/main/java/com/mqy/olearning/
│       ├── common/                  统一响应体、业务异常、全局异常处理
│       ├── config/                  MyBatis-Plus、跨域与拦截器、OpenAPI
│       ├── security/                JWT 工具、拦截器、@RequireRole、UserContext
│       ├── controller/              Auth / Public / PublicFile / Student / Study /
│       │                            Teacher / Admin / File / Export（9 个）
│       ├── service/                 16 个业务服务（含 DocPreview、Export、QuestionIO、CourseWork）
│       ├── mapper/                  MyBatis-Plus Mapper
│       ├── entity/                  20 张表对应实体
│       ├── dto/                     请求 / 响应对象
│       └── utils/                   密码加盐工具
├── olearning-web/                   前端（Vue 3 + Vite + TS）
│   └── src/
│       ├── views/                   首页、课程广场、课程详情、学习、测验、成绩、
│       │                            收藏与笔记、讨论区、我的课程、个人中心、登录/注册
│       ├── views/teacher/           学情看板、课程、章节课件、题库、试卷、作业批阅、学生
│       ├── views/admin/             平台统计、用户管理、课程审核、分类、公告
│       ├── layouts/                 PortalLayout（前台）、AdminLayout（教师/管理后台共用）
│       ├── components/              FileUploader（统一上传）、FilePreview（预览 + PPT 放映）、
│       │                            RichEditor（富文本 + 粘贴图片）
│       ├── utils/                   request（axios 封装）、file、chart、cover、quiz
│       ├── styles/theme.css          全站唯一配色/间距来源
│       ├── router/                  路由 + 角色守卫
│       └── stores/                  Pinia 登录态
├── sql/
│   ├── init.sql                     建库 + 20 张表 + 演示数据（含 5 门真实课程）
│   ├── upgrade-v2.sql               学习增强：收藏 / 笔记表 + 真实课程数据
│   ├── upgrade-v3.sql               课件预览、选课审核、批阅、导入导出
│   ├── upgrade-v4.sql               作业/测验时间窗、笔记挂作业试卷
│   └── upgrade-v5.sql               学号登录、试卷发布、章节挂载、讨论「有帮助」
├── scripts/
│   ├── smoke-test.sh                接口冒烟测试（8 组场景，可重复执行）
│   └── import_courses.py            把 upload/course 下的资料批量导入成课程（生成 SQL 到 build/）
├── build/import_courses.sql         课程导入脚本产物
├── docs/
│   ├── screenshots/                 联调验收截图（首页、学习页、测验、教师端、管理端）
│   ├── import/                      批量导入素材（用户 CSV、题库 xlsx 模板实例）
│   ├── 实训报告/                     实训报告、图表源稿与生成脚本
│   └── 优秀界面截图.docx              界面风格参考
├── docker-compose.yml               容器化一键部署
└── README.md
```

## 快速启动（本地开发）

### 1. 准备数据库

```bash
# 启动 MySQL 服务后执行（会先 DROP 再重建 online_learning 库）
mysql -u root -p --default-character-set=utf8mb4 < sql/init.sql
```

已有旧库不要重建，按需执行升级脚本（均为幂等，可重复执行）：

```bash
mysql -u root -p --default-character-set=utf8mb4 online_learning < sql/upgrade-v5.sql
```

> 必须带 `utf8mb4` 客户端参数，否则脚本里的中文注释会报 `Incorrect string value`。

### 2. 配置后端

仓库里的 `application.yml` **不含真实密码**，密码有两个来源，按优先级：

| 方式 | 做法 | 适用 |
|---|---|---|
| 本地配置文件（默认） | 新建 `olearning-server/config/application-local.yml`，填 `spring.datasource.password` | 自己机器上开发，已加入 `.gitignore` |
| 环境变量 | `export DB_PASSWORD=你的密码` | 容器 / 服务器 |

`config/application-local.yml` 模板：

```yaml
spring:
  datasource:
    username: root
    password: 你的MySQL密码
```

注意两点：

- 必须放在 `olearning-server/config/` 下（Spring Boot 的 `./config/` 自动搜索路径），
  **不要**放进 `src/main/resources` —— 那样会被打进 jar，容器里的 `DB_PASSWORD` 会被它覆盖。
- 启动时的工作目录要是 `olearning-server`，否则找不到 `./config/`，此时退回读 `DB_PASSWORD`。

### 3. 启动后端

```bash
cd olearning-server
mvn.cmd spring-boot:run        # Git Bash 下必须用 mvn.cmd
```

启动日志里应看到 `falling back to 1 default profile: "local"`，说明本地配置已加载。

- 服务地址：http://localhost:8080
- 接口文档：http://localhost:8080/swagger-ui.html

### 4. 启动前端

```bash
cd olearning-web
npm install
npm run dev
```

访问 http://localhost:5173 （Vite dev 只监听 `::1`，请用 `localhost` 而非 `127.0.0.1`）。

## 演示账号

**登录用「学号 / 职工号」，不是用户名**（v5 起 `sys_user.user_no` 全平台唯一）：

| 角色 | 登录账号（学号/职工号） | 用户名 | 密码 | 说明 |
|---|---|---|---|---|
| 管理员 | `A000001` | `admin` | `123456` | 用户管理、课程审核、分类、公告、统计 |
| 教师 | `T000001` | `teacher1` | `123456` | 课程/章节/题库/试卷发布/作业批阅/学情 |
| 教师 | `T000002` | `teacher2` | `123456` | 名下有一门待审核课程 |
| 学生 | `20210001` | `student1` | `123456` | 已选多门课，有成绩与测验记录 |
| 学生 | `20210002` | `student2` | `123456` | 未选课 |

注册 / 导入的账号同样以学号（学生）或职工号（教师）登录；教师自助注册后 `status = 0`，
需管理员在「用户管理」启用（侧栏有数字徽标，统计页有提醒条）。

## 核心业务闭环

```
教师建课(草稿) → 提交审核(待审) → 管理员审核通过(已发布)
    → 学生选课 enrollment（可开选课审核，待审核→通过/拒绝）
    → 逐章学习 + 「学完本章」打卡 study_progress，课件在线预览
    → 作业/测验挂在章节或整门课，教师设置「开始 ~ 截止」时间窗
    → 试卷需「发布」后学生可见（重新发布 version+1，历史答卷绑定 paper_version）
    → 学生作答（草稿自动保存、只能提交一次）→ 客观题自动判分 + 主观题教师批阅
    → 成绩与测验页按课程筛选汇总 → 学情看板 / 收藏与笔记 / 讨论区
```

## 迭代记录

### v5（2026-10-27 ~ 10-29）学号登录 · 试卷发布 · 章节挂载

| # | 能力 | 关键实现 |
|---|---|---|
| 1 | 学号 / 职工号登录 | `sys_user.user_no` 唯一索引，登录 DTO 字段为 `account`；注册校验学号格式与唯一性，存量账号按角色前缀回填 |
| 2 | 收藏与笔记页 | `favorite` 唯一约束（user_id, course_id）；`/api/my/favorites`、`/api/my/notes`、`/api/my/notes/groups`、`/api/my/overview`；前端 `Favorites.vue` 收藏 + 笔记分组浏览 |
| 3 | 讨论区「有帮助」 | `discussion_helpful(post_id, user_id)` 唯一索引兜底，`discussion_post.helpful_count` 冗余计数，每人每帖只能点一次 |
| 4 | 试卷发布机制 | `exam_paper.publish_status/published_at/version`；学生只看到已发布试卷；重新发布 `version+1`，`exam_record.paper_version` 区分答卷属于哪一轮 |
| 5 | 作业 / 测验挂章节 | `assignment.chapter_id`、`exam_paper.chapter_id`，为空表示整门课；章节页与课程详情均可定位 |
| 6 | 课程封面文字遮挡 | 卡片有真实封面图（`cover` 存在）时不再叠加课程名/简介白字，渐变兜底样式保留；涉及课程广场、首页、我的课程、收藏页 |
| 7 | 逐题解析可读性 | `ExamResultVO` 明细带 `options`；`utils/quiz.ts` 把字母答案翻译成「B. n/2」、判断题 T/F→正确/错误，选项按对错标色 |
| 8 | 成绩页统一筛选 | 课程标签驱动全页（平均分、最高分、及格率、趋势、等级分布）重新计算，顶部带筛选范围条 |
| 9 | 管理端待启用提醒 | `GET /api/admin/users/pending-teachers` + 侧栏徽标 + 统计页提醒条 |

### v4（2026-10-27）学生端体验 · 时间窗 · 全格式预览

| # | 能力 | 关键实现 |
|---|---|---|
| 1 | 封面 / 头像选文件 | 建课、编辑课程封面改为 `FileUploader` 选择图片 + 缩略图预览；个人中心头像同样可上传 |
| 2 | 富文本 + 粘贴图片 | 自制 `RichEditor.vue`（加粗/斜体/下划线/删除线/标题/列表/引用/代码/撤销/插图）；剪贴板图片自动上传后插入 `<img>`；接入笔记、作业作答、问答作答 |
| 3 | 作业/测验只能提交一次 | `AssignmentService.submit`、`ExamService.submit` Service 层拦截；前端已提交态变只读详情 |
| 4 | 作答自动保存 | localStorage 草稿 `ol-draft:assignment:{id}` / `ol-draft:exam:{paperId}`，输入防抖 800~1000ms，空作答不落盘 |
| 5 | 课程详情「作业测试」页签 | `GET /api/public/courses/{id}/works` → 作业 + 测验 + 统计，状态 未开始/进行中/已完成/已批阅/已截止 |
| 6 | 作业 / 测验时间窗 | `assignment.start_time`、`exam_paper.start_time/end_time`；学生只能窗口内作答，后端 `checkWindow` 兜底，未开放项前端置灰 |
| 7 | 题库导入导出 | 表头 `题型,题干,选项A~F,答案,参考答案,解析,难度`（xlsx，模板带「填写说明」sheet），逐行校验返回失败原因 |
| 8 | 全格式原样预览 + PPT 放映 | `DocPreviewService` 扩到旧二进制 doc/ppt/xls（HWPF/HSLF/HSSF）；`FilePreview` 支持 pptx 全屏放映（方向键/空格、3·5·8 秒自动播放、Esc 退出） |
| 9 | 弹窗遮挡彻底修复 | 全站 `el-dialog` / `el-drawer` 一律 `append-to-body`，脱离父容器层叠上下文 |

### v3（2026-10-27）课件预览 · 选课审核 · 题型与批阅

| # | 能力 | 关键实现 |
|---|---|---|
| 1 | 课件在线预览 | PDF 走 iframe 原生渲染；docx→段落块、pptx→逐页（含图片 base64）、xlsx→工作表；`/api/public/files/preview\|raw\|download?rid=` |
| 2 | 填空题 / 问答题 | 题型 1 单选 2 多选 3 判断 **4 填空** **5 问答**；填空 `\|` 分空、`~` 同空多写法、忽略大小写与全角空格；问答不自动判分，落 `exam_mark` 由教师打分 |
| 3 | 选课审核 + 名单导入 | `enrollment.status` 0 退课 / 1 在读 / **2 待审核** / 3 已拒绝；教师一键审批 + 按学号批量导入已注册学生 |
| 4 | 下载与导出权限 | `course.resource_downloadable` 关下载（`/files/download` 返回 403）；`export_scope` = `NONE` / `ENROLLED` / `ALL` 控制试卷、作业导出 docx |
| 5 | 管理员批量导入用户 | `/api/admin/users/import` 逐行校验（学号、用户名 4-20 位、角色、初始密码），返回逐行结果；`/users/import-template` 下载 UTF-8 BOM 模板 |
| 6 | 统一「选择文件上传」 | `FileUploader.vue` 接入章节课件、章节视频、作业附件、作业模板、封面、头像等全部上传位 |

### v2（2026-10-26）学习增强与真实课程

- 新增 `favorite`（课程收藏）、`study_note`（学习笔记）两张表与 `StudyService` / `StudyController`。
- `JwtInterceptor` 对 `/api/public/**` 改为「尽力解析」：游客可浏览，登录用户能拿回 `enrolled/progress`。
- 导入计算机网络、数据结构、操作系统、数据库系统综合实验等真实课程与配套作业、题库、试卷。

## 数据库表（20 张）

| 分组 | 表 |
|---|---|
| 账号与组织 | `sys_user`（含 `user_no` 学号/职工号）、`course_category` |
| 课程与学习 | `course`、`chapter`、`course_resource`、`enrollment`、`study_progress` |
| 作业与测验 | `assignment`、`submission`、`question`、`exam_paper`、`paper_question`、`exam_record`、`exam_mark`（主观题批阅明细） |
| 互动与运营 | `discussion_post`、`discussion_helpful`、`discussion_reply`、`sys_notice` |
| 学习增强 | `favorite`、`study_note` |

## 角色权限设计

- 登录态由 JWT 承载，请求头 `Authorization: Bearer <token>`；文件直链可用 `?token=` 兜底
- `JwtInterceptor` 解析 token 写入 `UserContext`（ThreadLocal），`@RequireRole("TEACHER")` 标注 Controller
- 越权返回 `403`，未登录返回 `401`，前端 axios 拦截器统一跳登录页

## 接口冒烟测试

```bash
bash scripts/smoke-test.sh                      # 默认 http://localhost:8080
bash scripts/smoke-test.sh http://127.0.0.1:8080
```

覆盖 8 组场景：认证、前台公开接口、权限（401/403）、学生端（选课/打卡/答题/交卷判分/防作弊）、
教师端、管理端、学习增强（概览/收藏/笔记/游客课件），脚本可重复执行。

## 容器化部署（可选）

```bash
docker compose up -d
```

- 前端：http://localhost:8081
- 后端：http://localhost:8080
- MySQL 映射到宿主机 3307，避免与本机 3306 冲突；首次启动自动执行 `sql/init.sql`
- 容器内后端读 `DB_PASSWORD` 环境变量

## 验收情况

| 项目 | 结果 |
|---|---|
| 后端单元测试 | `mvn test` → 17 个用例（JWT、MD5、课程/选课/测验服务） |
| 接口冒烟测试 | 8 组场景，覆盖三角色登录与权限、判分、导入导出 |
| 前端构建 | `npm run build`（vue-tsc + vite build）通过；仅提示 chunk > 500KB 未拆包 |
| 浏览器实测 | agent-browser 走通课件预览、PPT 放映、批阅抽屉、批量导入、成绩筛选等关键路径 |

页面截图见 `docs/screenshots/`；实训报告与图表见 `docs/实训报告/`。

## 已知约束

- 密码采用 **MD5 + 随机盐**（按实训要求），生产环境应换 BCrypt
- 文件存储为本地磁盘 `upload/`，未做分布式与 CDN；视频用 `<video>` 播 MP4，不转码
- 课件预览为纯 Java 解析（不依赖 LibreOffice）：文字、表格与内嵌图片可还原，
  SmartArt、公式、复杂动画等版式不保证 100% 一致；单文件图片 base64 上限 4MB
- 判断课件类型以 `course_resource.file_url` 为准，展示名可以不带扩展名
- `scripts/smoke-test.sh` 的登录请求体仍是旧的 `username` 字段，需同步为 `account` + 学号/职工号
  （后端只认 `account`），否则断言会因 400 全部失败
- 未启用 Redis 缓存；高并发场景需补缓存、连接池与静态资源分离
