/*
 Navicat Premium Dump SQL

 Source Server         : WSL PostgreSQL
 Source Server Type    : PostgreSQL
 Source Server Version : 180001 (180001)
 Source Host           : 192.168.49.46:5432
 Source Catalog        : testdb
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 180001 (180001)
 File Encoding         : 65001

 Date: 08/01/2026 15:17:47
*/


-- ----------------------------
-- Sequence structure for oorder_order_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."oorder_order_id_seq";
CREATE SEQUENCE "public"."oorder_order_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for user_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_id_seq";
CREATE SEQUENCE "public"."user_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for dispatcher
-- ----------------------------
DROP TABLE IF EXISTS "public"."dispatcher";
CREATE TABLE "public"."dispatcher" (
  "dispatcher_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "dispatcher_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "dispatcher_phone" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Records of dispatcher
-- ----------------------------
INSERT INTO "public"."dispatcher" VALUES ('010100', '摇摆羊', '13365789765');
INSERT INTO "public"."dispatcher" VALUES ('010101', '小亮', '15878977898');
INSERT INTO "public"."dispatcher" VALUES ('1000011', '老八', '13526777887');
INSERT INTO "public"."dispatcher" VALUES ('10111', '赵三金', '15965578765');

-- ----------------------------
-- Table structure for fastfood_shop
-- ----------------------------
DROP TABLE IF EXISTS "public"."fastfood_shop";
CREATE TABLE "public"."fastfood_shop" (
  "shop_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "price" int4 NOT NULL,
  "m_sale_v" int4 NOT NULL
)
;
COMMENT ON COLUMN "public"."fastfood_shop"."price" IS '价格';
COMMENT ON COLUMN "public"."fastfood_shop"."m_sale_v" IS '销售量';

-- ----------------------------
-- Records of fastfood_shop
-- ----------------------------
INSERT INTO "public"."fastfood_shop" VALUES ('二食堂鸡公煲', 25, 100);
INSERT INTO "public"."fastfood_shop" VALUES ('天美自选饭', 10, 200);
INSERT INTO "public"."fastfood_shop" VALUES ('天美馋嘴鸭', 16, 61);
INSERT INTO "public"."fastfood_shop" VALUES ('小美烤盘饭', 12, 101);
INSERT INTO "public"."fastfood_shop" VALUES ('小美章丘炒鸡', 16, 99);
INSERT INTO "public"."fastfood_shop" VALUES ('小美麻辣烫', 15, 103);
INSERT INTO "public"."fastfood_shop" VALUES ('小美负一楼蛋炒饭', 10, 102);
INSERT INTO "public"."fastfood_shop" VALUES ('至美猪脚饭', 20, 53);

-- ----------------------------
-- Table structure for oorder
-- ----------------------------
DROP TABLE IF EXISTS "public"."oorder";
CREATE TABLE "public"."oorder" (
  "order_id" int4 NOT NULL DEFAULT nextval('oorder_order_id_seq'::regclass),
  "shop_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "order_money" int4 NOT NULL,
  "order_way" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "cons_phone" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "cons_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "cons_addre" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "checked" int4 DEFAULT 0,
  "create_time" varchar(50) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Records of oorder
-- ----------------------------
INSERT INTO "public"."oorder" VALUES (1, '大美鸡公煲', 12, '人工订餐', '13525188888', '老三', '15公寓', 0, '2022-12-16 14:35:17');
INSERT INTO "public"."oorder" VALUES (2, '小美负一楼蛋炒饭', 10, '人工订餐', '13525188888', '吴方', '10公寓', 2, '2022-12-16 14:35:26');
INSERT INTO "public"."oorder" VALUES (3, '至美猪脚饭', 20, '人工订餐', '13525188888', '吴方', '8公寓', 0, '2022-12-16 14:35:35');

-- ----------------------------
-- Table structure for orderway
-- ----------------------------
DROP TABLE IF EXISTS "public"."orderway";
CREATE TABLE "public"."orderway" (
  "orderway_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "count" int4 NOT NULL
)
;
COMMENT ON COLUMN "public"."orderway"."orderway_name" IS '订餐方式';
COMMENT ON COLUMN "public"."orderway"."count" IS '该种方式的订餐数量';

-- ----------------------------
-- Records of orderway
-- ----------------------------
INSERT INTO "public"."orderway" VALUES ('网上订餐', 51);
INSERT INTO "public"."orderway" VALUES ('人工订餐', 111);

-- ----------------------------
-- Table structure for server
-- ----------------------------
DROP TABLE IF EXISTS "public"."server";
CREATE TABLE "public"."server" (
  "service_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "service_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "fastfood_shop_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "public"."server"."service_id" IS '服务员编号';
COMMENT ON COLUMN "public"."server"."fastfood_shop_name" IS '所在的店铺名字';

-- ----------------------------
-- Records of server
-- ----------------------------
INSERT INTO "public"."server" VALUES ('100000', '刀哥', '小美烤盘饭');
INSERT INTO "public"."server" VALUES ('100001', '司徒王朗', '小美麻辣烫');
INSERT INTO "public"."server" VALUES ('100011', '虎哥', '大美鸡公煲');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS "public"."user";
CREATE TABLE "public"."user" (
  "id" int4 NOT NULL DEFAULT nextval('user_id_seq'::regclass),
  "username" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "telephone" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "role" int4 NOT NULL
)
;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO "public"."user" VALUES (2, 'CMX', 'Aa.460734909', '18682412818', 1);
INSERT INTO "public"."user" VALUES (3, 'YCX', 'aA.460734909', '18873190398', 0);

-- ----------------------------
-- Table structure for user_msg
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_msg";
CREATE TABLE "public"."user_msg" (
  "id" int4 NOT NULL,
  "real_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sex" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "age" int4 NOT NULL,
  "mail" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "phone" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "user_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Records of user_msg
-- ----------------------------
INSERT INTO "public"."user_msg" VALUES (2, 'cmx', '男', 18, '320836@qq.com', '18682412818', 'CMX');
INSERT INTO "public"."user_msg" VALUES (3, 'ycx', '男', 20, '787898@qq.com', '18873190398', 'YCX');

-- ----------------------------
-- Table structure for wuliu
-- ----------------------------
DROP TABLE IF EXISTS "public"."wuliu";
CREATE TABLE "public"."wuliu" (
  "order_id" int4 NOT NULL,
  "cons_phone" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "disp_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "deliver_time" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "ended" int4 NOT NULL DEFAULT 0
)
;
COMMENT ON COLUMN "public"."wuliu"."order_id" IS '订单的编号';
COMMENT ON COLUMN "public"."wuliu"."ended" IS '是否结束';

-- ----------------------------
-- Records of wuliu
-- ----------------------------
INSERT INTO "public"."wuliu" VALUES (2, '13525112345', '010100', '20分钟', 0);

-- ----------------------------
-- Function structure for order_delete_func
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."order_delete_func"();
CREATE FUNCTION "public"."order_delete_func"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
  UPDATE orderway
  SET count = count - 1
  WHERE orderway.orderway_name = OLD.order_way;
  RETURN OLD;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for order_delete_sale_func
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."order_delete_sale_func"();
CREATE FUNCTION "public"."order_delete_sale_func"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
  UPDATE fastfood_shop
  SET m_sale_v = m_sale_v - 1
  WHERE fastfood_shop.shop_name = OLD.shop_name;
  RETURN OLD;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for order_insert_func
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."order_insert_func"();
CREATE FUNCTION "public"."order_insert_func"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
  UPDATE orderway 
  SET count = count + 1
  WHERE orderway.orderway_name = NEW.order_way;
  RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for order_insert_sale_func
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."order_insert_sale_func"();
CREATE FUNCTION "public"."order_insert_sale_func"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
  UPDATE fastfood_shop
  SET m_sale_v = m_sale_v + 1
  WHERE fastfood_shop.shop_name = NEW.shop_name;
  RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for order_update_func
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."order_update_func"();
CREATE FUNCTION "public"."order_update_func"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
  IF NEW.order_way != OLD.order_way THEN
    UPDATE orderway SET count = count - 1 WHERE orderway.orderway_name = OLD.order_way;
    UPDATE orderway SET count = count + 1 WHERE orderway.orderway_name = NEW.order_way;
  END IF;
  RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for wuliu_insert_func
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."wuliu_insert_func"();
CREATE FUNCTION "public"."wuliu_insert_func"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
  UPDATE oorder
  SET checked = 1
  WHERE oorder.order_id = NEW.order_id;
  RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- View structure for sended_order
-- ----------------------------
DROP VIEW IF EXISTS "public"."sended_order";
CREATE VIEW "public"."sended_order" AS  SELECT oorder.order_id,
    oorder.shop_name,
    oorder.order_money,
    oorder.order_way,
    oorder.cons_phone,
    oorder.cons_name,
    oorder.cons_addre,
    wuliu.disp_id,
    wuliu.deliver_time,
    dispatcher.dispatcher_phone
   FROM oorder
     JOIN wuliu ON oorder.order_id = wuliu.order_id
     JOIN dispatcher ON wuliu.disp_id::text = dispatcher.dispatcher_id::text
  WHERE oorder.checked = 2;

-- ----------------------------
-- View structure for sending_order
-- ----------------------------
DROP VIEW IF EXISTS "public"."sending_order";
CREATE VIEW "public"."sending_order" AS  SELECT oorder.order_id,
    oorder.shop_name,
    oorder.order_money,
    oorder.order_way,
    oorder.cons_phone,
    oorder.cons_name,
    oorder.cons_addre,
    wuliu.disp_id,
    wuliu.deliver_time,
    dispatcher.dispatcher_phone
   FROM oorder
     JOIN wuliu ON oorder.order_id = wuliu.order_id
     JOIN dispatcher ON wuliu.disp_id::text = dispatcher.dispatcher_id::text
  WHERE oorder.checked = 1;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."oorder_order_id_seq"
OWNED BY "public"."oorder"."order_id";
SELECT setval('"public"."oorder_order_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_id_seq"
OWNED BY "public"."user"."id";
SELECT setval('"public"."user_id_seq"', 1, false);

-- ----------------------------
-- Indexes structure for table dispatcher
-- ----------------------------
CREATE UNIQUE INDEX "idx_dispatcher_id" ON "public"."dispatcher" USING btree (
  "dispatcher_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_dispatcher_name" ON "public"."dispatcher" USING btree (
  "dispatcher_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_dispatcher_phone" ON "public"."dispatcher" USING btree (
  "dispatcher_phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table dispatcher
-- ----------------------------
ALTER TABLE "public"."dispatcher" ADD CONSTRAINT "dispatcher_pkey" PRIMARY KEY ("dispatcher_id");

-- ----------------------------
-- Indexes structure for table fastfood_shop
-- ----------------------------
CREATE INDEX "idx_fastfood_shop_m_sale_v" ON "public"."fastfood_shop" USING btree (
  "m_sale_v" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_fastfood_shop_price" ON "public"."fastfood_shop" USING btree (
  "price" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "idx_fastfood_shop_shop_name" ON "public"."fastfood_shop" USING btree (
  "shop_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table fastfood_shop
-- ----------------------------
ALTER TABLE "public"."fastfood_shop" ADD CONSTRAINT "fastfood_shop_pkey" PRIMARY KEY ("shop_name");

-- ----------------------------
-- Indexes structure for table oorder
-- ----------------------------
CREATE INDEX "idx_oorder_checked" ON "public"."oorder" USING btree (
  "checked" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oorder_cons_addre" ON "public"."oorder" USING btree (
  "cons_addre" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oorder_cons_name" ON "public"."oorder" USING btree (
  "cons_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oorder_cons_phone" ON "public"."oorder" USING btree (
  "cons_phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oorder_create_time" ON "public"."oorder" USING btree (
  "create_time" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "idx_oorder_order_id" ON "public"."oorder" USING btree (
  "order_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oorder_order_money" ON "public"."oorder" USING btree (
  "order_money" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oorder_order_way" ON "public"."oorder" USING btree (
  "order_way" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_oorder_shop_name" ON "public"."oorder" USING btree (
  "shop_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table oorder
-- ----------------------------
CREATE TRIGGER "order_delete" AFTER DELETE ON "public"."oorder"
FOR EACH ROW
EXECUTE PROCEDURE "public"."order_delete_func"();
CREATE TRIGGER "order_delete_sale" AFTER DELETE ON "public"."oorder"
FOR EACH ROW
EXECUTE PROCEDURE "public"."order_delete_sale_func"();
CREATE TRIGGER "order_insert" AFTER INSERT ON "public"."oorder"
FOR EACH ROW
EXECUTE PROCEDURE "public"."order_insert_func"();
CREATE TRIGGER "order_insert_sale" AFTER INSERT ON "public"."oorder"
FOR EACH ROW
EXECUTE PROCEDURE "public"."order_insert_sale_func"();
CREATE TRIGGER "order_update" AFTER UPDATE ON "public"."oorder"
FOR EACH ROW
EXECUTE PROCEDURE "public"."order_update_func"();

-- ----------------------------
-- Primary Key structure for table oorder
-- ----------------------------
ALTER TABLE "public"."oorder" ADD CONSTRAINT "oorder_pkey" PRIMARY KEY ("order_id");

-- ----------------------------
-- Indexes structure for table orderway
-- ----------------------------
CREATE INDEX "idx_orderway_count" ON "public"."orderway" USING btree (
  "count" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "idx_orderway_orderway_name" ON "public"."orderway" USING btree (
  "orderway_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table orderway
-- ----------------------------
ALTER TABLE "public"."orderway" ADD CONSTRAINT "orderway_pkey" PRIMARY KEY ("orderway_name");

-- ----------------------------
-- Indexes structure for table server
-- ----------------------------
CREATE INDEX "idx_server_fastfood_shop_name" ON "public"."server" USING btree (
  "fastfood_shop_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "idx_server_service_id" ON "public"."server" USING btree (
  "service_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_server_service_name" ON "public"."server" USING btree (
  "service_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table server
-- ----------------------------
ALTER TABLE "public"."server" ADD CONSTRAINT "server_pkey" PRIMARY KEY ("service_id");

-- ----------------------------
-- Indexes structure for table user
-- ----------------------------
CREATE UNIQUE INDEX "idx_user_id" ON "public"."user" USING btree (
  "id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_password" ON "public"."user" USING btree (
  "password" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_role" ON "public"."user" USING btree (
  "role" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_telephone" ON "public"."user" USING btree (
  "telephone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_username" ON "public"."user" USING btree (
  "username" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table user
-- ----------------------------
ALTER TABLE "public"."user" ADD CONSTRAINT "user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_msg
-- ----------------------------
CREATE INDEX "idx_user_msg_age" ON "public"."user_msg" USING btree (
  "age" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_msg_id" ON "public"."user_msg" USING btree (
  "id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_msg_mail" ON "public"."user_msg" USING btree (
  "mail" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_msg_phone" ON "public"."user_msg" USING btree (
  "phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_msg_real_name" ON "public"."user_msg" USING btree (
  "real_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_msg_sex" ON "public"."user_msg" USING btree (
  "sex" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_msg_user_name" ON "public"."user_msg" USING btree (
  "user_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Indexes structure for table wuliu
-- ----------------------------
CREATE INDEX "idx_wuliu_cons_phone" ON "public"."wuliu" USING btree (
  "cons_phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_wuliu_deliver_time" ON "public"."wuliu" USING btree (
  "deliver_time" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_wuliu_disp_id" ON "public"."wuliu" USING btree (
  "disp_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_wuliu_ended" ON "public"."wuliu" USING btree (
  "ended" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "idx_wuliu_order_id" ON "public"."wuliu" USING btree (
  "order_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table wuliu
-- ----------------------------
CREATE TRIGGER "wuliu_insert" AFTER INSERT ON "public"."wuliu"
FOR EACH ROW
EXECUTE PROCEDURE "public"."wuliu_insert_func"();

-- ----------------------------
-- Primary Key structure for table wuliu
-- ----------------------------
ALTER TABLE "public"."wuliu" ADD CONSTRAINT "wuliu_pkey" PRIMARY KEY ("order_id");

-- ----------------------------
-- Foreign Keys structure for table user_msg
-- ----------------------------
ALTER TABLE "public"."user_msg" ADD CONSTRAINT "fk_user_msg_user" FOREIGN KEY ("id") REFERENCES "public"."user" ("id") ON DELETE CASCADE ON UPDATE CASCADE;
