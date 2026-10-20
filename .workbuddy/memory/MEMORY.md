# 在线学习平台（online_learning_system）项目约定

## 工程结构
- `olearning-server/`：Spring Boot 后端，根包 `com.mqy.olearning`
- `olearning-web/`：Vue 3 前端（尚未开始）
- 仓库根即 `D:\mqy\java\SpringBoot\online_learning_system`，`.gitignore` 在根

## 版本与技术栈
- Spring Boot **3.5.16**（3.x 线最新 GA，勿跳 4.x，本机 Java 17）
- Java 17 / Maven 3.9.11 / MySQL 9.2 / Redis 3.0.504（均未升级，按开发规划锁定）
- mysql-connector-j 锁 **8.4.0**；MySQL 9 排序规则 `utf8mb4_0900_ai_ci`
- 持久层 MyBatis-Plus，不用 Hibernate（规避 MySQL 9 方言）

## 本机命令约定
- Git Bash 下必须用 **`mvn.cmd`**，裸 `mvn` 报 classworlds Launcher 错误
- 终端里有注入变量 `SERVER__PORT`，跑 Spring Boot 前需 `env -u SERVER__PORT`
- curl 探测本机服务加 `--noproxy '*'`

## 协作方式（用户 2026-10-20 明确重申，务必遵守）
- **只教学，不代写**：不生成项目骨架/业务代码文件。只给操作步骤、讲原理、说清该改哪个文件、给验证命令。
- **逐步推进**：一次只讲一步，做完等用户反馈再进下一步。每一步都要用户自己动手参与。
- 极少数纯查询/体检命令（查版本、看端口）可代为执行并汇报结果。
