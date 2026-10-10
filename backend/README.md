# backend — Flask 后端（对照实现）

原课程设计的 Flask 版本，**完整保留**用于与 Spring Boot 版做逐接口对照
（见 `tools/compare_api.ps1`），两者响应逐字节兼容。

## 内容

- `app.py` — 全部路由与业务逻辑（约 30 个接口）
- `auth.py` — JWT 生成与解析（PyJWT / HS256 / issuer=wufang）
- `config.py` — 数据库 / Redis 配置（支持环境变量覆盖）
- `db/init.sql` — 容器初始化：8 张表 + 2 个视图 + 6 个触发器 + 示例数据
- `Dockerfile` / `docker-compose.yml` — python:3.10-slim 镜像；MySQL + Redis + 后端编排
- `wait-for-mysql.sh` — 等待 MySQL 就绪后启动服务
- `requirements.txt` — Python 依赖（Flask 2.3 / SQLAlchemy / PyMySQL / PyJWT / redis）

> `main.py` 为 PyCharm 自动生成的示例文件，未使用（可删）。

启动方式见根目录 `README.md` 与 `docs/RUNBOOK.md`。
