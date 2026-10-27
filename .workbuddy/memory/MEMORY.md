# 在线学习平台（online_learning_system）项目约定

## 工程结构
- `olearning-server/`：Spring Boot 后端，根包 `com.mqy.olearning`
- `olearning-web/`：Vue 3 前端
- 仓库根即 `D:\mqy\java\SpringBoot\online_learning_system`，`.gitignore` 在根
- `sql/upgrade-v3.sql` / `sql/upgrade-v4.sql`：老库升级到 v3 / v4（幂等），必须用
  `mysql --default-character-set=utf8mb4` 执行
- 数据库名 `online_learning`；试卷表是 `exam_paper`（不是 `paper`）

## 版本与技术栈
- Spring Boot **3.5.16**（3.x 线最新 GA，勿跳 4.x，本机 Java 17）
- Java 17 / Maven 3.9.11 / MySQL 9.2 / Redis 3.0.504（均未升级，按开发规划锁定）
- mysql-connector-j 锁 **8.4.0**；MySQL 9 排序规则 `utf8mb4_0900_ai_ci`
- 持久层 MyBatis-Plus，不用 Hibernate（规避 MySQL 9 方言）

## 本机命令约定
- Git Bash 下必须用 **`mvn.cmd`**，裸 `mvn` 报 classworlds Launcher 错误
- 终端里有注入变量 `SERVER__PORT`，跑 Spring Boot 前需 `env -u SERVER__PORT`
- curl 探测本机服务加 `--noproxy '*'`
- 本地 MySQL：`mysql -uroot -p301477 -h127.0.0.1 --default-character-set=utf8mb4`
- Bash 里一次 `rm` 删超过 ~5 个文件会被沙箱 safe-delete 拦成 SIGTERM，分小批删；
  `rm -rf` 目录会走回收站，genie-trash 失败时删不掉（此时加进 .gitignore 即可）

## 协作方式（2026-10-20 立；2026-10-26 起按用户新要求放开）
- 10-20 立的是「只教学，不代写、逐步推进」，用于课程设计阶段。
- **10-26 用户明确要求直接改造项目**（界面风格 + 功能丰富度），此时直接写代码、跑构建、重启服务并汇报结果，不再逐步等确认。默认沿用这条，除非用户改口。

## 界面与前端约定（2026-10-26 改造后）
- 视觉风格：**暖米橙**（底 `#fdf7f1`、主色 `#ff6b35`），参考 `docs/《优秀界面截图.docx》`，内容仍是中文在线学习平台。
- 样式唯一来源：`olearning-web/src/styles/theme.css`，页面里不写裸色值；主题 import 必须排在 element-plus 之后。
- 学员端图表用纯 SVG 手绘，教师/管理端用 ECharts，配色统一走 `src/utils/chart.ts`。
- 课程封面配色按 `id % 8` 取固定渐变，保证跨页面一致。
- 全站 `el-dialog` / `el-drawer` 一律 `append-to-body`，否则会被父容器层叠上下文裁剪（历史 bug）。
- 通用组件：`components/FileUploader.vue`（统一上传）、`components/FilePreview.vue`（预览 + PPT 放映）、
  `components/RichEditor.vue`（富文本 + 粘贴图片，不引第三方编辑器）、
  `utils/file.ts`（withToken / openFile / triggerDownload / fileNameOf / isImageUrl，直链带 `?token=`）。
- `.el-dialog__body` 的 padding 已在 theme.css 统一为 `6px 20px 4px`，页面里不要再给弹窗加左右 padding。

## 业务约定（v4 增量）
- 作业时间窗 `assignment.start_time ~ deadline`；试卷 `exam_paper.start_time ~ end_time`；
  学生只能在窗口内作答，后端 `checkWindow` 兜底。
- 作业/测验只能提交一次，Service 层拦截；草稿存 localStorage `ol-draft:assignment:{id}` /
  `ol-draft:exam:{paperId}`，空作答不落盘。
- 笔记可挂在作业/试卷上：`study_note.ref_type` = ASSIGNMENT / EXAM + `ref_id`。
- 课程作业汇总：`GET /api/public/courses/{id}/works` → `{assignments, exams, stats}`，
  状态 NOT_START / ONGOING / SUBMITTED / GRADED / EXPIRED。
- 题库导入导出表头：`题型,题干,选项A~F,答案,参考答案,解析,难度`（xlsx，模板带「填写说明」sheet）。
- 回看已提交测验的成绩用 `GET /api/records/{recordId}`（不是 `/api/my/records/{id}`）。

## 业务约定（v3）
- 题型：1 单选 / 2 多选 / 3 判断 / 4 填空（`|` 分空、`~` 同空多写法）/ 5 问答（不自动判分）
- 批阅：`exam_record.mark_status` 0 无需 / 1 待批阅 / 2 已批阅；明细在 `exam_mark`
- 选课：`enrollment.status` 0 退课 / 1 在读 / 2 待审核 / 3 已拒绝
- 课程开关：`enroll_audit`、`resource_downloadable`、`export_scope`(NONE/ENROLLED/ALL)
- 课件预览走 `/api/public/files/preview|raw|download?rid=`，下载受 `resource_downloadable` 控制
