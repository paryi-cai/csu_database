-- 契约回归测试用最小表结构（H2 MySQL 兼容模式）
-- 目的：在没有 MySQL / Docker 的机器上也能验证接口契约与"数据库副作用"。
-- 真实表结构仍以 backend/db/init.sql 为准，本文件不修改任何生产脚本。
--
-- 已覆盖批次 2 需要的对象：user / user_msg / fastfood_shop / oorder / orderway /
-- dispatcher / wuliu，以及视图 sending_order、sended_order 与触发器
-- order_insert_sale / order_delete_sale（用于验证"下单后销量由数据库自增，
-- Java 不重复实现"）。
--
-- 语句分隔符为 @@（见 application-h2test.yml 的 spring.sql.init.separator），
-- 因为触发器体内部含分号，Spring 默认按分号切分会把语句截断。

drop trigger if exists order_insert_sale@@
drop trigger if exists order_insert@@
drop trigger if exists order_delete_sale@@
drop trigger if exists order_delete@@
drop trigger if exists wuliu_insert@@
drop view if exists sending_order@@
drop view if exists sended_order@@
drop table if exists wuliu@@
drop table if exists oorder@@
drop table if exists orderway@@
drop table if exists server@@
drop table if exists dispatcher@@
drop table if exists user_msg@@
drop table if exists fastfood_shop@@
drop table if exists user@@

create table user
(
    id        int unsigned not null auto_increment,
    username  varchar(20)  not null,
    password  varchar(20)  not null,
    telephone varchar(20)  not null,
    role      int          not null,
    primary key (id)
)@@

create table user_msg
(
    id        int unsigned,
    real_name varchar(50) not null,
    sex       varchar(50) not null,
    age       int         not null,
    mail      varchar(50) not null,
    phone     varchar(50) not null,
    user_name varchar(50) not null,
    constraint fk_user_msg_user foreign key (id) references user (id) on delete cascade on update cascade
)@@

create table fastfood_shop
(
    shop_name varchar(50) not null,
    price     int         not null,
    m_sale_v  int         not null,
    primary key (shop_name)
)@@

create table oorder
(
    order_id    int         not null auto_increment,
    shop_name   varchar(50) not null,
    order_money int         not null,
    order_way   varchar(50) not null,
    cons_phone  varchar(50) not null,
    cons_name   varchar(50) not null,
    cons_addre  varchar(50) not null,
    checked     int         null default 0,
    create_time varchar(50) null default null,
    primary key (order_id)
)@@

create table orderway
(
    orderway_name varchar(50) not null,
    count         int         not null,
    primary key (orderway_name)
)@@

-- 刻意不建 server.fastfood_shop_name -> fastfood_shop.shop_name 外键：
-- 生产库（backend/db/init.sql）**没有**这个外键，实测 information_schema 只有
-- user_msg.id -> user.id 一条外键。夹具不能比生产"更严格"，否则测试会锁住一个
-- 生产上不成立的行为（删店铺会因为外键失败，而生产上会删除成功并留下 server 孤儿行）。
create table server
(
    service_id         varchar(50) not null,
    service_name       varchar(50) not null,
    fastfood_shop_name varchar(50) not null,
    primary key (service_id)
)@@

create table dispatcher
(
    dispatcher_id    varchar(50) not null,
    dispatcher_name  varchar(50) not null,
    dispatcher_phone varchar(50) not null,
    primary key (dispatcher_id)
)@@

create table wuliu
(
    order_id     int         not null,
    cons_phone   varchar(50) not null,
    disp_id      varchar(50) not null,
    deliver_time varchar(50) not null,
    ended        int         not null default 0,
    primary key (order_id)
)@@

-- 视图：与 init.sql 中的定义保持一致（三表连接 + checked 过滤）。
-- 注意：这里**刻意不额外加列**。曾经为了让订单列表能按 create_time 排序而给测试视图补过
-- create_time，结果 H2 全绿、生产库却报 Unknown column——因为 init.sql 的视图只 select 了
-- 10 个字段，本就没有 create_time。测试夹具一旦比生产"更全"，就会把这类问题放过去。
create view sended_order as
select o.order_id         as order_id,
       o.shop_name        as shop_name,
       o.order_money      as order_money,
       o.order_way        as order_way,
       o.cons_phone       as cons_phone,
       o.cons_name        as cons_name,
       o.cons_addre       as cons_addre,
       w.disp_id          as disp_id,
       w.deliver_time     as deliver_time,
       d.dispatcher_phone as dispatcher_phone
from oorder o
         join wuliu w on o.order_id = w.order_id
         join dispatcher d on w.disp_id = d.dispatcher_id
where o.checked = 2@@

create view sending_order as
select o.order_id         as order_id,
       o.shop_name        as shop_name,
       o.order_money      as order_money,
       o.order_way        as order_way,
       o.cons_phone       as cons_phone,
       o.cons_name        as cons_name,
       o.cons_addre       as cons_addre,
       w.disp_id          as disp_id,
       w.deliver_time     as deliver_time,
       d.dispatcher_phone as dispatcher_phone
from oorder o
         join wuliu w on o.order_id = w.order_id
         join dispatcher d on w.disp_id = d.dispatcher_id
where o.checked = 1@@

-- 关于触发器：
-- 生产库中的 6 个触发器（order_insert / order_update / order_delete /
-- order_insert_sale / order_delete_sale / wuliu_insert）由 MySQL 负责执行。
-- H2 1.4.200 的 CREATE TRIGGER 只支持 "AS CALL <java方法>" 形式，无法直接表达
-- MySQL 的 begin...end 触发器体，因此本测试库不重建触发器。
--
-- 因此本测试覆盖的是：SQL 语句本身、字段映射、响应结构，以及"Java 不重复实现
-- 触发器逻辑"这一职责边界；触发器副作用（销量增减、checked 置 1）需在真实
-- MySQL 上验证（见 README 的验收步骤与 tools/compare_api 脚本）。
--
-- 唯一在 H2 上保留验证的数据库侧行为是 oorder.checked 的默认值 0（见
-- UserApiMutationTest），因为 Java 侧插入语句刻意不写该列。
