# mysql2mariadb_datax_json — DataX：MySQL → MariaDB

8 张表各一个 DataX 任务配置（`m2m_*.json`），目标 `localhost:3308/testdb`。

- 读取：`mysqlreader`（`localhost:3307/caimingxuan`）
- 写入：`mysqlwriter` — DataX 官方没有 MariaDB 插件，MariaDB 兼容 MySQL 协议，直接复用；
  `writeMode: replace` 并在 `preSql` 中先 `DELETE FROM`
- 账号密码为 DataX AES 密文；更换凭据用 `python datax.py -e "明文"` 重新生成

运行方式见根 `README.md`「跨数据库数据迁移（DataX）」与 `docs/RUNBOOK.md` 第 9 章。
