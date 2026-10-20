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
│       ├── views/portal       首页、课程广场、详情、学习、测验、成绩、讨论
│       ├── views/teacher      学情看板、课程、章节、题库、试卷、作业、学生
│       ├── views/admin        统计、用户、审核、分类、公告
│       ├── layouts/           前台布局、后台管理布局
│       ├── router/            路由 + 角色守卫
│       ├── stores/            Pinia 登录态
│       └── utils/             Axios 封装
├── sql/init.sql               建库 + 16 张表 + 演示数据
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

修改 `olearning-server/src/main/resources/application.yml` 里的 `password` 为你的 MySQL 密码，
或设置环境变量 `DB_PASSWORD`。

### 3. 启动后端

```bash
cd olearning-server
mvn.cmd spring-boot:run        # Git Bash 下必须用 mvn.cmd
```

- 服务地址：http://localhost:8080
- 接口文档：http://localhost:8080/swagger-ui.html

### 4. 启动前端

```bash
cd olearning-web
npm install
npm run dev
```

访问 http://localhost:5173

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

## 角色权限设计

- 登录态由 JWT 承载，请求头 `Authorization: Bearer <token>`
- `JwtInterceptor` 解析 token 写入 `UserContext`（ThreadLocal）
- `@RequireRole("TEACHER")` 注解标注在 Controller 上，拦截器比对角色
- 越权访问返回 `403`，未登录返回 `401`，前端拦截器统一跳登录页

## 数据库表（16 张）

`sys_user`、`course_category`、`course`、`chapter`、`course_resource`、`enrollment`、
`study_progress`、`assignment`、`submission`、`question`、`exam_paper`、`paper_question`、
`exam_record`、`discussion_post`、`discussion_reply`、`sys_notice`

## 已知约束

- 密码采用 **MD5 + 随机盐**（按实训要求），生产环境应换 BCrypt
- 文件存储为本地磁盘 `upload/`，可平滑迁移 MinIO / OSS
- 视频直接使用 `<video>` 播放 MP4，不做转码
- 未引入 Redis：缓存为加分项，为避免本机 Redis 3.0 兼容问题暂不启用
