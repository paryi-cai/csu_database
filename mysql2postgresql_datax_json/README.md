# mysql2postgresql_datax_json — DataX：MySQL → PostgreSQL

8 张表各一个 DataX 任务配置（`m2p_*.json`），目标 `localhost:5432/testdb`。

- 读取：`mysqlreader`（`localhost:3307/caimingxuan`）
- 写入：`postgresqlwriter`，`preSql: truncate table ... cascade`
- 列引用使用 PG 的 `"` 引号（跨库 SQL 方言差异之一）
- 账号密码为 DataX AES 密文

迁移结果见 `../other_database/PostgreSQL.sql`（用 plpgsql 函数 + 触发器复刻了 MySQL 的触发器逻辑）。
