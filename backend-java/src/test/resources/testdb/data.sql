-- 测试数据。语句分隔符为 @@（见 application-h2test.yml）。
-- 账号与 database/caimingxuan.sql 中的示例账号一致（README 给出的测试账号）。
--
-- 关于订单时间：order 1..3 使用固定历史日期（2022-12-16），这是既有列表类契约测试
-- 逐字节断言依赖的值，不能改成动态日期。
-- 统计接口（/api/manager/stats/**）按 [today-(days-1), today] 聚合，需要一条落在窗口内的样本，
-- 但**不加在本文件里**：ManagerApiReadContractTest 对未派发列表做的是响应体逐字节比对，
-- 多一行数据就会破坏既有断言。因此改由 ManagerStatsApiTest 在用例内自行插入一条带当天日期的
-- 订单（测试库 oorder 没有外键约束，cons_phone 用 init.sql 里的普通顾客手机号即可）。

insert into user (id, username, password, telephone, role)
values (2, 'CMX', 'Aa.460734909', '18682412818', 1)@@
insert into user (id, username, password, telephone, role)
values (3, 'YCX', 'aA.460734909', '18873190398', 0)@@

insert into user_msg (id, real_name, sex, age, mail, phone, user_name)
values (2, 'cmx', '男', 18, '320836@qq.com', '18682412818', 'CMX')@@
insert into user_msg (id, real_name, sex, age, mail, phone, user_name)
values (3, 'ycx', '男', 20, '787898@qq.com', '18873190398', 'YCX')@@

insert into fastfood_shop (shop_name, price, m_sale_v)
values ('大美鸡公煲', 15, 100)@@
insert into fastfood_shop (shop_name, price, m_sale_v)
values ('天美自选饭', 10, 200)@@

insert into dispatcher (dispatcher_id, dispatcher_name, dispatcher_phone)
values ('010100', '摇摆羊', '13365789765')@@
insert into dispatcher (dispatcher_id, dispatcher_name, dispatcher_phone)
values ('010101', '小亮', '15878977898')@@

-- 服务员：'大美鸡公煲' 被引用，用于验证"被外键引用的店铺不能删除"
insert into server (service_id, service_name, fastfood_shop_name)
values ('100011', '虎哥', '大美鸡公煲')@@

insert into orderway (orderway_name, count)
values ('人工订餐', 108)@@
insert into orderway (orderway_name, count)
values ('网上订餐', 51)@@

-- YCX（18873190398）：1 条未派发订单
insert into oorder (order_id, shop_name, order_money, order_way, cons_phone, cons_name, cons_addre, checked, create_time)
values (1, '大美鸡公煲', 15, '人工订餐', '18873190398', '吴方', '15公寓', 0, '2022-12-16 14:35:17')@@

-- YCX：1 条派送中订单（checked=1 且 wuliu 有记录，会被 sending_order 视图查出）
insert into oorder (order_id, shop_name, order_money, order_way, cons_phone, cons_name, cons_addre, checked, create_time)
values (2, '天美自选饭', 10, '人工订餐', '18873190398', '吴方', '8公寓', 1, '2022-12-16 14:35:26')@@

insert into wuliu (order_id, cons_phone, disp_id, deliver_time, ended)
values (2, '18873190398', '010100', '20分钟', 0)@@

-- YCX：1 条已送达订单（checked=2 且 wuliu 有记录，会被 sended_order 视图查出）
insert into oorder (order_id, shop_name, order_money, order_way, cons_phone, cons_name, cons_addre, checked, create_time)
values (3, '大美鸡公煲', 15, '网上订餐', '18873190398', '吴方', '10公寓', 2, '2022-12-16 15:01:00')@@

insert into wuliu (order_id, cons_phone, disp_id, deliver_time, ended)
values (3, '18873190398', '010100', '15分钟', 1)@@

-- CMX（18682412818）刻意不放订单，用于验证"空列表"响应
