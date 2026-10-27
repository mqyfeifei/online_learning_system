# 在线学习平台（Online Learning System）

计算机软件综合开发实训项目 · 基于 Spring Boot 3 + Vue 3 的前后端分离在线学习平台。

## 技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.5.16 |
| 持久层 | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | 9.2（驱动 mysql-connector-j 8.4.0） |
| 认证 | JWT（jjwt） | 0.12.6 |
| 接口文档 | springdoc OpenAPI | 2.8.6 |
| 前端框架 | Vue 3 + Vite + TypeScript | Vue 3.5 / Vite 6 |
| UI 组件 | Element Plus | 2.8 |
| 状态与路由 | Pinia + Vue Router 4 | — |
| 图表 | ECharts | 5.5 |
| 文档解析 | Apache POI（poi-ooxml） | 5.2.5 |
| 构建 | Maven 3.9.11 / Node 22 | — |

## 目录结构

```
online_learning_system/
├── olearning-server/          后端（Spring Boot）
│   └── src/main/java/com/mqy/olearning/
│       ├── common/            统一响应体、业务异常、全局异常处理
│       ├── config/            MyBatis-Plus、跨域与拦截器、接口文档
│       ├── security/          JWT 工具、拦截器、角色注解、用户上下文
│       ├── controller/        认证 / 前台 / 学生 / 教师 / 管理 / 文件
│       ├── service/           业务逻辑
│       ├── mapper/            MyBatis-Plus Mapper
│       ├── entity/            16 张表对应实体
│       ├── dto/               请求/响应对象
│       └── utils/             密码加盐工具
├── olearning-web/             前端（Vue 3）
│   └── src/
│       ├── views/             前台页面（首页、课程广场、详情、学习、测验、成绩、讨论、个人中心）
│       ├── views/teacher      教师端：学情看板、课程、章节、题库、试卷、作业、学生
│       ├── views/admin        管理端：统计、用户、审核、分类、公告
│       ├── layouts/           前台布局、后台管理布局
│       ├── router/            路由 + 角色守卫
│       ├── stores/            Pinia 登录态
│       └── utils/             Axios 封装
├── sql/init.sql               建库 + 17 张表 + 演示数据
├── sql/upgrade-v3.sql         老库升级到 v3（幂等，可重复执行）
├── sql/upgrade-v4.sql         老库升级到 v4（幂等，可重复执行）
├── scripts/smoke-test.sh      接口冒烟测试（27 项断言）
├── docs/screenshots/          联调验收截图
├── docker-compose.yml         容器化一键部署
└── README.md
```

## 快速启动（本地开发）

### 1. 准备数据库

```bash
# 启动 MySQL 服务后执行（会先 DROP 再重建 online_learning 库）
mysql -u root -p < sql/init.sql
```

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

访问 http://localhost:5173

## 联调验收结果

在以下环境实测跑通（2026-10-20）：

| 项目 | 版本 / 结果 |
|---|---|
| JDK | 17.0.12 |
| Maven | 3.9.11 |
| Node | 22.20.0 |
| MySQL | 9.2.0（`utf8mb4_0900_ai_ci`） |
| Spring Boot | 3.5.16 |
| 后端单元测试 | `mvn test` → 17 个用例全部通过 |
| 接口冒烟测试 | `bash scripts/smoke-test.sh` → 27 项断言全部通过 |
| 前端生产构建 | `npm run build` → 通过（仅提示 chunk > 500KB，未拆包） |

冒烟脚本覆盖：三角色登录、错误密码、未登录 401、越权 403、重复选课拦截、选课 / 打卡 /
答题 / 交卷判分、教师端 4 个接口、管理端 3 个接口，以及「学生答题响应不得出现 answer 字段」的防作弊校验。

页面截图见 `docs/screenshots/`：首页、学习页、在线测验、教师学情看板、教师课程管理、
管理端统计、用户管理。

## 容器化部署（可选）

```bash
docker compose up -d
```

- 前端：http://localhost:8081
- 后端：http://localhost:8080
- MySQL 映射到宿主机 3307，避免与本机 3306 冲突
- 首次启动自动执行 `sql/init.sql`

## 演示账号

| 角色 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 管理员 | `admin` | `123456` | 用户管理、课程审核、分类、公告、统计 |
| 教师 | `teacher1` | `123456` | 课程/章节/题库/试卷/作业批阅/学情 |
| 教师 | `teacher2` | `123456` | 名下有一门待审核课程 |
| 学生 | `student1` | `123456` | 已选《Java 程序设计基础》，学完 2 章 |
| 学生 | `student2` | `123456` | 未选课 |

## 核心业务闭环

```
教师建课(草稿) → 提交审核(待审) → 管理员审核通过(已发布)
    → 学生选课 enrollment
    → 逐章学习 + 「学完本章」打卡 study_progress
    → 参加随堂测验 exam_record（自动判分 + 错题解析）
    → 提交作业 submission → 教师批阅打分
    → 学情看板 / 我的成绩统计
```

## v3 功能清单（2026-10-27 迭代）

老库升级（幂等，可重复执行）：

```bash
mysql -u root -p --default-character-set=utf8mb4 online_learning < sql/upgrade-v3.sql
```

> 必须用 `utf8mb4` 客户端，否则存储过程里的中文注释会报 `Incorrect string value`。

| # | 能力 | 关键实现 |
|---|---|---|
| 1 | 课件在线预览（PDF / PPT / DOCX / XLSX / 图片 / 音视频） | 后端 `DocPreviewService` 用 POI 把 docx→段落块、pptx→逐页（含图片 base64）、xlsx→工作表；PDF 走 iframe 原生渲染。前端 `FilePreview.vue` 统一弹窗，下载按钮受课程开关控制 |
| 2 | 弹窗被遮挡修复 | 全站 `el-dialog` / `el-drawer` 加 `append-to-body`（共 13 处），脱离父级层叠上下文 |
| 3 | 填空题 / 问答题 | 题型 1 单选 2 多选 3 判断 **4 填空** **5 问答**；填空按 `|` 分空、`~` 同空多写法、忽略大小写与全角空格；问答不自动判分，落 `exam_mark` 由教师打分 |
| 4 | 统一「选择文件上传」 | `FileUploader.vue` 封装 `el-upload`，已接入章节课件、章节视频、作业附件、作业模板等所有上传位 |
| 5 | 选课审核 + 名单导入 | `enrollment.status` 扩展为 0 退课 / 1 在读 / **2 待审核** / 3 已拒绝；教师端一键审批 + 按登录名批量导入已注册学生 |
| 6 | 下载与导出权限 | 课程 `resource_downloadable` 控制课件下载（关闭后 `/files/download` 返回 403）；`export_scope` 取 `NONE` / `ENROLLED` / `ALL` 控制试卷、作业导出为 docx |
| 7 | 管理员批量导入用户 | `/api/admin/users/import` 逐行校验（用户名 4-20 位字母数字下划线、角色合法、手机号可选），返回逐行成功/失败原因；`/users/import-template` 下载 UTF-8 BOM 的 CSV 模板 |
| 8 | 体验增强 | 学生端看到「待教师批阅 / 已批阅 + 教师评语」，教师端答卷列表支持查看与改分，作业可带附件模板，学习页课件卡片直接点击查看 |

新增依赖：`org.apache.poi:poi-ooxml:5.2.5`（后端 `pom.xml`）。

## v4 功能清单（2026-10-27 第二轮迭代）

老库升级（幂等，可重复执行）：

```bash
mysql -u root -p --default-character-set=utf8mb4 online_learning < sql/upgrade-v4.sql
```

| # | 能力 | 关键实现 |
|---|---|---|
| 1 | 封面 / 头像可选文件 | 建课、编辑课程弹窗的封面改为 `FileUploader` 选择图片 + 缩略图预览；个人中心头像同样可上传 |
| 2 | 个人中心名片头像被遮挡 | `.id-bg` 显式 `z-index: 0`、`.id-body` 抬到 `z-index: 1` + `margin-top: -38px`，橙黄渐变带不再压住头像 |
| 3 | 富文本编辑 + 粘贴图片 | 自制 `RichEditor.vue`（加粗/斜体/下划线/删除线/标题/列表/引用/代码块/撤销/插图）；剪贴板图片自动上传后插入 `<img>`，外部 HTML 降级为纯文本。接入学习笔记、作业作答、问答题作答、作业笔记 |
| 4 | 作业/测验只能提交一次 | 后端 `AssignmentService.submit`、`ExamService.submit` 在 Service 层拦截重复提交；前端已提交态变只读详情 + 「已完成」标记 + 「作业笔记」入口 |
| 5 | 作答自动保存 | localStorage 草稿 `ol-draft:assignment:{id}` / `ol-draft:exam:{paperId}`，输入防抖 800~1000ms 保存，关闭弹窗 / 组件卸载 / 点「保存并退出」时落盘，重新进入自动恢复 |
| 6 | 学生端导出入口 | 学习页与课程详情页的作业、测验卡片直接给「导出」按钮，按课程 `export_scope` 决定是否显示（`NONE` 隐藏、`ENROLLED` 仅选课学生、`ALL` 平台用户） |
| 7 | 课程详情「作业测试」页签 | 与课程介绍 / 目录 / 课件并列；`GET /api/public/courses/{id}/works` 一次返回作业 + 测验 + 完成统计，状态分 未开始 / 进行中 / 已完成 / 已批阅 / 已截止 |
| 8 | 组卷 / 批阅弹窗彻底修复 | `el-drawer` / `el-dialog` 全部补 `append-to-body`，组卷面板不再被 `#FDF7F1` 背景裁切 |
| 9 | 题库导入导出 | `GET /courses/{id}/questions/export`、`GET /questions/import-template`、`POST /courses/{id}/questions/import`；表头 `题型,题干,选项A~F,答案,参考答案,解析,难度`，逐行校验并返回失败原因 |
| 10 | 作业 / 测验时间窗 | `assignment.start_time`、`exam_paper.start_time/end_time`；教师端可设开始与结束，学生只能在窗口内作答（后端 `checkWindow` 兜底） |
| 11 | 全格式原样预览 + PPT 放映 | `DocPreviewService` 扩到旧二进制 `doc/ppt/xls`（poi-scratchpad：HWPF / HSLF / HSSF）；`PublicFileController` 支持 `rid` 与 `url` 双寻址，作业附件、笔记图片、头像都能预览；`FilePreview` 对 pptx 新增全屏放映（方向键 / 空格翻页、3·5·8 秒自动播放、Esc 退出） |

新增依赖：`org.apache.poi:poi-scratchpad:5.2.5`。

## 角色权限设计

- 登录态由 JWT 承载，请求头 `Authorization: Bearer <token>`
- `JwtInterceptor` 解析 token 写入 `UserContext`（ThreadLocal）
- `@RequireRole("TEACHER")` 注解标注在 Controller 上，拦截器比对角色
- 越权访问返回 `403`，未登录返回 `401`，前端拦截器统一跳登录页

## 数据库表（17 张）

`sys_user`、`course_category`、`course`、`chapter`、`course_resource`、`enrollment`、
`study_progress`、`assignment`、`submission`、`question`、`exam_paper`、`paper_question`、
`exam_record`、`exam_mark`（v3 新增·主观题批阅明细）、`discussion_post`、`discussion_reply`、`sys_notice`

## 已知约束

- 密码采用 **MD5 + 随机盐**（按实训要求），生产环境应换 BCrypt
- 文件存储为本地磁盘 `upload/`，可平滑迁移 MinIO / OSS
- 视频直接使用 `<video>` 播放 MP4，不做转码
- 课件预览为纯 Java 解析（未依赖 LibreOffice）：docx / pptx / xlsx 可还原文字、表格与内嵌图片，
  但 SmartArt、公式、复杂动画等版式不保证 100% 一致；单文件图片 base64 上限 4MB
- 未引入 Redis：缓存为加分项，为避免本机 Redis 3.0 兼容问题暂不启用
