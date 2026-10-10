-- 幂等补齐 README / 验收脚本使用的测试账号。
--
-- 背景：database/init.sql（容器初始化脚本）里的示例账号是 iu / mty，
--       与 README 与验收脚本使用的 CMX / YCX 不一致。
--       为遵守"不修改 init.sql"的约束，改为在容器启动后执行本脚本补齐账号。
--
-- 用法（由根目录 start_project.bat 自动执行）：
--   docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools/seed_test_accounts.sql
--
-- 本脚本可重复执行：INSERT ... ON DUPLICATE KEY UPDATE 保证幂等。
-- 注意：本文件必须保持 LF 换行（Windows 的 CRLF 会让 mysql 客户端报语法错误）。

-- 管理员 CMX（role=1 → 登录后进入管理端）
INSERT INTO `user` (`id`, `username`, `password`, `telephone`, `role`)
VALUES (2, 'CMX', 'Aa.460734909', '18682412818', 1)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`),
                        `password` = VALUES(`password`),
                        `telephone` = VALUES(`telephone`),
                        `role` = VALUES(`role`);

-- 普通用户 YCX（role=0 → 登录后进入用户端）
INSERT INTO `user` (`id`, `username`, `password`, `telephone`, `role`)
VALUES (3, 'YCX', 'aA.460734909', '18873190398', 0)
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`),
                        `password` = VALUES(`password`),
                        `telephone` = VALUES(`telephone`),
                        `role` = VALUES(`role`);

-- 个人信息（user_msg 通过 phone 查询，且 id 外键关联 user.id）
INSERT INTO `user_msg` (`id`, `real_name`, `sex`, `age`, `mail`, `phone`, `user_name`)
SELECT 2, 'cmx', '男', 18, '320836@qq.com', '18682412818', 'CMX'
WHERE NOT EXISTS (SELECT 1 FROM `user_msg` WHERE `phone` = '18682412818');

INSERT INTO `user_msg` (`id`, `real_name`, `sex`, `age`, `mail`, `phone`, `user_name`)
SELECT 3, 'ycx', '男', 20, '787898@qq.com', '18873190398', 'YCX'
WHERE NOT EXISTS (SELECT 1 FROM `user_msg` WHERE `phone` = '18873190398');
