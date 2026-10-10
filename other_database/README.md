# other_database — 迁移结果导出

- `MariaDB.sql` — MySQL → MariaDB（12.1.2）迁移后的完整导出
- `PostgreSQL.sql` — MySQL → PostgreSQL（18.1）迁移后的完整导出；
  PostgreSQL 版本使用 plpgsql 函数 + 触发器复刻了 MySQL 的触发器逻辑

> 这两个数据库只在跨库迁移演示时需要启动；系统运行只依赖 MySQL 与 Redis。
