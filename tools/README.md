# tools — 根级验收与数据脚本

| 脚本 | 用途 |
| --- | --- |
| `compare_api.ps1` | Flask ↔ Spring Boot **逐接口响应对照**（HTTP 状态码、语义 JSON、规范化后的响应体原文；十六进制大小写归一） |
| `seed_test_accounts.sql` | 幂等补齐测试账号 CMX / YCX（容器初始化账号是 iu / mty） |
| `gen_seed_demo_stats.ps1` | 生成「统计看板演示数据」SQL 的脚本（近 N 天批量订单，纯 INSERT + UPDATE，无 DDL） |
| `seed_demo_stats.sql` | 上述脚本的生成结果，可直接执行（**注意文件头写的执行顺序**） |

用法见根 `README.md` 的验收章节与 `docs/RUNBOOK.md`。
