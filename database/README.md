# database — 数据库完整导出

- `caimingxuan.sql` — 库 `caimingxuan` 的完整导出：
  - **8 张表**：`user` / `user_msg` / `fastfood_shop` / `oorder` / `orderway` / `server` / `dispatcher` / `wuliu`
  - **2 个视图**：`sending_order`（checked=1）、`sended_order`（checked=2），由 oorder × wuliu × dispatcher 三表连接
  - **6 个触发器**：`order_insert/update/delete`（维护 orderway.count）、`order_insert_sale/order_delete_sale`（维护店铺销量）、`wuliu_insert`（派发后置 checked=1）
  - 含演示数据与测试账号

- `init.sql` — 容器**首次启动自动执行**的初始化脚本（结构、视图、触发器与 `caimingxuan.sql` 同源；
  由 `backend-java/docker-compose.yml` 挂载进 MySQL 容器）

> `caimingxuan.sql` 用于手工导入 MySQL / Navicat 或查阅完整对象定义；两者结构一致，日常改库后以 `caimingxuan.sql` 为准重新导出。
