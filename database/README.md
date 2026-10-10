# database — 数据库完整导出

- `caimingxuan.sql` — 库 `caimingxuan` 的完整导出：
  - **8 张表**：`user` / `user_msg` / `fastfood_shop` / `oorder` / `orderway` / `server` / `dispatcher` / `wuliu`
  - **2 个视图**：`sending_order`（checked=1）、`sended_order`（checked=2），由 oorder × wuliu × dispatcher 三表连接
  - **6 个触发器**：`order_insert/update/delete`（维护 orderway.count）、`order_insert_sale/order_delete_sale`（维护店铺销量）、`wuliu_insert`（派发后置 checked=1）
  - 含演示数据与测试账号

> 容器首次启动实际执行的是 `backend/db/init.sql`（结构与数据同源）；
> 本文件用于手工导入 MySQL / Navicat 或查阅完整对象定义。
