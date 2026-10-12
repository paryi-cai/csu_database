# backend-java — Spring Boot 3 后端（主后端）

校园快餐订餐管理系统后端：Spring Boot 3.4.4 + MyBatis + Druid + Redis + JWT，
提供用户端与管理员两组 REST 接口，配套 **102 个接口契约测试**（H2，无需 Docker）。

## 内容

- `src/main/java/com/csu/takeout/`
  - `controller/` — user/ 与 manager/ 两组接口（含异常告警：`manager/ManagerAlertController`）
  - `service/ (+impl)` / `mapper/` — 业务层与 MyBatis 持久层
  - `json/` — 统一 JSON 序列化器（空格排版、末尾换行、非 ASCII 转义，保证响应格式稳定）
  - `interceptor/ context/` — JWT 拦截器（请求头 `token`）+ ThreadLocal 当前用户
  - `task/` — 定时任务（`ConsistencyCheckTask` 数据一致性巡检，默认关闭）
  - `entity/ dto/ vo/ result/ handler/ config/ constant/ properties/ utils`
- `src/main/resources/` — `application.yml`（默认 `localhost:3307`）+ `mapper/*.xml`（SQL 全部在这里）
- `src/test/` — **102 个接口契约测试**（H2 MySQL 兼容模式，无需 Docker / MySQL）
- `tools/build.ps1` — 一键构建（受限环境自动使用可写 Maven 仓库副本）
- `tools/verify-wait-probe.sh` — 验证「端口就绪探测」行为的辅助脚本
- `Dockerfile` / `docker-compose.yml` — 多阶段构建（Maven → JRE 17）；MySQL 3307 / Redis 6379 / API 5000

## 异常检测与告警（可选）

- 运行期未预期异常 → 按异常类节流后异步发告警邮件（SMTP，默认关闭、失败只写日志）。
- 数据一致性巡检 → 对比源库 MySQL 与 DataX 目标库（MariaDB / PostgreSQL）行数，异常见告警、恢复发通知；接口：`POST /api/manager/alert/test`、`POST /api/manager/alert/patrol`。
- 启用方式与环境变量清单见根目录 README「异常检测与告警」一节。

## 构建与测试

```powershell
powershell -ExecutionPolicy Bypass -File tools\build.ps1            # 构建 + 全部测试
powershell -ExecutionPolicy Bypass -File tools\build.ps1 -SkipTests # 只打包
```
