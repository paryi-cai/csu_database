# 运行手册（RUNBOOK）

> 校园快餐订餐管理系统 —— 从零开始把「数据库 + Java 后端 + Vue 前端」跑起来，直到在浏览器里看到页面。
>
> 本手册中的所有命令、端口、路径、报错与排查结论都经过实机验证（Windows + PowerShell 5.1）。
> 建议按顺序执行；**第 0 章先看一遍**，能避免 80% 的卡点。

---

## 目录

- [第 0 章 全景：这个项目由什么组成](#第-0-章-全景这个项目由什么组成)
- [第 1 章 环境准备（装什么、怎么验证）](#第-1-章-环境准备装什么怎么验证)
- [第 2 章 方式一：一键启动](#第-2-章-方式一一键启动)
- [第 3 章 方式二：手工分步启动（推荐用于理解与排查）](#第-3-章-方式二手工分步启动推荐用于理解与排查)
- [第 4 章 用 Navicat 看数据库](#第-4-章-用-navicat-看数据库)
- [第 5 章 用 IDEA 跑后端 / 跑前端](#第-5-章-用-idea-跑后端--跑前端)
- [第 6 章 页面验收清单](#第-6-章-页面验收清单)
- [第 7 章 触发器与视图演示脚本（面试重点）](#第-7-章-触发器与视图演示脚本面试重点)
- [第 8 章 接口契约测试](#第-8-章-接口契约测试)
- [第 9 章 可选：DataX 跨数据库迁移复现](#第-9-章-可选datax-跨数据库迁移复现)
- [第 9.6 节 迁移后一致性校验与告警](#96-迁移后一致性校验与告警可选)
- [第 10 章 停止 / 重启 / 彻底重置](#第-10-章-停止--重启--彻底重置)
- [第 11 章 排查表（实测踩过的所有坑）](#第-11-章-排查表实测踩过的所有坑)
- [附录 A 端口与账号速查](#附录-a-端口与账号速查)
- [附录 B 环境实测记录](#附录-b-环境实测记录)

---

## 第 0 章 全景：这个项目由什么组成

```
┌─────────────────────────────────────────────────────────────┐
│  浏览器  http://localhost:8080                               │
│     │                                                        │
│     ▼                                                        │
│  Vue 前端 (frontend/sjk)          ← 需要 Node.js             │
│     │  Axios，baseURL 写死 http://127.0.0.1:5000             │
│     │  自定义请求头 token（不是 Authorization: Bearer）        │
│     ▼                                                        │
│  Java 后端 (backend-java)         ← 需要 JDK 17（或 Docker）  │
│     │  Spring Boot 3.4.4，监听 5000                          │
│     ▼                                                        │
│  MySQL 8.0 (localhost:3307)       ← 需要 Docker              │
│     库名 caimingxuan                                         │
│     ├── 8 张表                                               │
│     ├── 2 个视图   sending_order / sended_order              │
│     └── 6 个触发器 负责维护订单状态、订餐方式计数、店铺销量      │
│                                                              │
│  Redis (localhost:6379)           ← 需要 Docker（当前无接口依赖）│
└─────────────────────────────────────────────────────────────┘
```

**四个关键认知**（能省掉很多困惑）：

1. **只需要一个数据库**：MySQL。`other_database/`、`mysql2mariadb_datax_json/`、`mysql2postgresql_datax_json/` 是"跨库迁移实验"的产物与配置，**系统运行时不使用**，里面的 MariaDB / PostgreSQL 不需要启动。
2. **前端不需要编译**：`npm run serve` 起的是开发服务器，改代码即时热更新，不需要 `npm run build`。
3. **订单状态由数据库负责**：`oorder.checked`（0 未派发 / 1 派送中 / 2 已送达）由触发器与视图维护，Java 代码里**没有**更新该字段的 SQL。详见第 7 章。
4. **后端为 Spring Boot 3（`backend-java/`）**：MySQL / Redis 由 Docker 提供，后端监听 5000；前端 Axios 直连 `127.0.0.1:5000`。

---

## 第 1 章 环境准备（装什么、怎么验证）

### 1.1 必需软件

| 软件 | 版本要求 | 用途 | 是否必需 |
|---|---|---|---|
| **Docker Desktop** | 任意较新版本 | 运行 MySQL 8.0 + Redis + Java 后端 | ✅ 必需 |
| **Node.js** | 18 或更高（**实测 v24.21.0 可用**） | 运行 Vue 前端 | ✅ 必需 |
| 浏览器 | Chrome / Edge | 访问页面 | ✅ 必需 |

### 1.2 可选软件（提高效率）

| 软件 | 用途 |
|---|---|
| IntelliJ IDEA | 阅读 / 调试 Java 后端、开前端项目 |
| Navicat / DataGrip | 可视化查看数据库、跑 SQL、演示触发器 |
| VS Code | 更轻量的前端编辑器 |
| JDK 17 + Maven | 想在 IDEA 里跑后端（而不是用 Docker）时需要 |

### 1.3 安装与验证

#### Docker Desktop

下载 <https://www.docker.com/products/docker-desktop/> → 安装（勾选 "Use WSL 2 instead of Hyper-V"）→ **启动 Docker Desktop**，等托盘鲸鱼图标静止。

```powershell
docker version
# 必须同时打印 Client 与 Server 两段。只有 Client 说明 Docker Desktop 没启动。
docker compose version
# 期望输出：Docker Compose version vX.Y.Z
```

> 若提示需要 WSL2：管理员 PowerShell 执行 `wsl --install`，重启电脑后再开 Docker Desktop。

#### Node.js

下载 <https://nodejs.org/en/download>（LTS 版；要 18 可去 <https://nodejs.org/dist/v18.20.4/>）。

> ⚠️ **便携版（zip）解压后必须把解压目录加入 PATH**，否则 `node` 命令不可用。

```powershell
node -v      # 期望 v18.x / v20.x / v24.x
npm -v       # 期望 9.x 以上
```

**如果 `npm -v` 报 `UnauthorizedAccess` / 无法加载脚本**：这是 PowerShell 执行策略为 `Restricted` 导致的（`npm` 实际走 `npm.ps1`）。二选一：

```powershell
# 方案 A：放开当前用户的脚本执行（推荐，一次性，不需管理员）
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

# 方案 B：不改策略，改用 npm.cmd
npm.cmd -v
npm.cmd install
npm.cmd run serve
```

**如果 `npm install` 报 `ERESOLVE`**：本项目已在 `frontend/sjk/.npmrc` 里配置了 `legacy-peer-deps=true` 与 `engine-strict=false`，正常情况下不会出现；若仍出现：

```powershell
npm install --legacy-peer-deps
```

---

## 第 2 章 方式一：一键启动

**前提**：Docker Desktop 已启动。

```powershell
cd "D:\Desktop\面试\数据库项目\csu_database-master"
.\start_project.bat
```

脚本按 6 步执行，每步都有输出：

| 步骤 | 屏幕输出 | 说明 |
|---|---|---|
| 1/6 | `Docker Desktop is running.` | 检查 Docker |
| 2/6 | `Volume is ready.` | 首次自动创建外部数据卷 `heu_db_mysql_data` |
| 3/6 | `Containers are up.` | 构建 Java 镜像 + 启动 MySQL/Redis/后端；**首次约 3~8 分钟**（下载基础镜像与依赖） |
| 4/6 | `MySQL is healthy.` | 等 MySQL 健康检查通过 |
| 5/6 | `Test accounts are ready.` | 幂等补齐测试账号 `CMX` / `YCX` |
| 6/6 | 弹出新窗口 | `npm install`（首次）+ `npm run serve` |

**成功标志**：

```
==========================================
  Project is starting successfully!

  Backend API : http://localhost:5000
  API docs    : http://localhost:5000/doc.html
  Frontend UI : http://localhost:8080
==========================================
```

然后直接跳到 [第 6 章 页面验收清单](#第-6-章-页面验收清单)。

---

## 第 3 章 方式二：手工分步启动（推荐用于理解与排查）

一次只做一件事，出问题容易定位。

### 步骤 1：启动数据库 + 后端

```powershell
cd "D:\Desktop\面试\数据库项目\csu_database-master"
docker volume create heu_db_mysql_data      # 仅首次；提示已存在可忽略
docker compose -p csu_db -f backend-java/docker-compose.yml up -d --build
```

**首次会看到**：拉取 `mysql:8.0`、`redis:7-alpine`、`maven:3.9.9-eclipse-temurin-17`、`eclipse-temurin:17-jre-jammy` 等镜像，然后容器内执行 `mvn package`。**3~8 分钟属正常**。

**检查状态**：

```powershell
docker compose -p csu_db -f backend-java/docker-compose.yml ps
```

期望：

| 服务 | 状态 |
|---|---|
| `csu_db-mysql-1` | `Up ... (healthy)` ← **`healthy` 很关键** |
| `csu_db-redis-1` | `Up` |
| `csu_db-backend-1` | `Up` |

**验证后端**：

```powershell
# 浏览器打开接口文档（Knife4j）
start http://localhost:5000/doc.html

# 命令行验证登录
$body = '{"userortel":"18873190398","password":"aA.460734909"}'
$login = Invoke-RestMethod -Uri http://localhost:5000/api/user/login -Method Post -Body $body -ContentType 'application/json'
$login
# 期望：code=200  msg=登录成功  token=...  role=0
```

### 步骤 2：补齐测试账号

`database/init.sql`（容器首次初始化脚本）里的示例账号是 `iu` / `mty`，与 README 使用的 `CMX` / `YCX` **不一致**。为不改动 `init.sql`，用仓库里的幂等脚本补齐：

```powershell
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools\seed_test_accounts.sql
```

验证：

```powershell
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan -e "select id,username,telephone,role from user"
# 期望看到 CMX(role=1) 与 YCX(role=0)
```

> 若 `csu_db-mysql-1` 不存在，先 `docker ps` 看实际容器名（项目名不同时名字会不同）。

### 步骤 3：启动前端

```powershell
cd frontend\sjk
npm install        # 仅首次；已装过可跳过
npm run serve
```

**成功标志**：

```
  App running at:
  - Local:   http://localhost:8080/
  - Network: http://192.168.x.x:8080/
```

> ⚠️ **这个窗口不要关**，关掉就等于前端停了。要同时跑 Java 和前端，就开两个终端窗口。

第一次编译可能看到一堆 `npm warn deprecated ...` 黄字，**属正常，忽略即可**。

---

## 第 4 章 用 Navicat 看数据库

### 4.1 新建连接（**只建一个**）

Navicat → **连接 → MySQL**：

| 字段 | 填什么 | 注意 |
|---|---|---|
| 连接名 | `csu_local`（随意） | |
| 主机 | `localhost` | |
| **端口** | **`3307`** | ⚠️ **不是 3306**。compose 把容器的 3306 映射到主机的 3307 |
| 用户名 | `root` | |
| 密码 | `123456` | |

点 **测试连接** → 成功 → 确定。

### 4.2 连上后应该看到什么

展开 `caimingxuan` 库：

```
caimingxuan
├── 表 (8)
│   ├── user            用户（id 自增、username、password、telephone、role）
│   ├── user_msg        用户详细信息（外键 → user.id）
│   ├── fastfood_shop   店铺（shop_name 主键、price、m_sale_v 销量）
│   ├── oorder          订单（order_id 自增、checked 状态、create_time 为字符串）
│   ├── orderway        订餐方式与数量统计
│   ├── server          服务员（外键 → fastfood_shop.shop_name）
│   ├── dispatcher      配送员
│   └── wuliu           物流记录（order_id 主键）
├── 视图 (2)
│   ├── sending_order   oorder ⋈ wuliu ⋈ dispatcher，where checked = 1
│   └── sended_order    同上，where checked = 2
└── 触发器 (6)
    ├── order_insert        AFTER INSERT oorder  → orderway.count + 1
    ├── order_insert_sale   AFTER INSERT oorder  → fastfood_shop.m_sale_v + 1
    ├── order_update        AFTER UPDATE oorder  → order_way 变化时调整计数
    ├── order_delete        AFTER DELETE oorder  → orderway.count − 1
    ├── order_delete_sale   AFTER DELETE oorder  → fastfood_shop.m_sale_v − 1
    └── wuliu_insert        AFTER INSERT wuliu   → oorder.checked = 1
```

### 4.3 确认测试账号

在查询窗口执行：

```sql
SELECT id, username, telephone, role FROM `user`;
-- 期望：CMX(role=1，管理端) 与 YCX(role=0，用户端)
```

### 4.4 看不到库 / 表怎么办

`init.sql` **只在数据卷为空时执行一次**。若数据卷里已有旧数据（或容器启动时初始化失败），手动画导入：

Navicat → 右键连接 → **运行 SQL 文件** → 选：

```
D:\Desktop\面试\数据库项目\csu_database-master\database\caimingxuan.sql
```

> ⚠️ 用这份而**不要**用 `database/init.sql`：前者自带 `CMX` / `YCX` 账号，后者是 `iu` / `mty`。
> 前者还是**完整导出**（含视图、触发器、外键与数据），一份就够。

---

## 第 5 章 用 IDEA 跑后端 / 跑前端

### 5.1 用 IDEA 跑 Java 后端

**关键：只起数据库容器，把后端让给 IDEA 跑**（否则 5000 端口冲突）。

```powershell
cd "D:\Desktop\面试\数据库项目\csu_database-master"
docker compose -p csu_db -f backend-java/docker-compose.yml up -d mysql redis
```

**IDEA 步骤**：

1. **Open** → 选 **`backend-java`** 这一层（**不要**打开最外层仓库目录，否则识别不到 `pom.xml`）→ Trust Project → 等 Maven 依赖下载完
2. **File → Project Structure** → Project SDK 选 JDK 17（`D:\develop\ms-17.0.20`），Language level = 17
3. **Settings → Build Tools → Maven**：确认 User settings file 指向你的 `settings.xml`（含阿里云镜像），Local repository 会自动读出
   > ⚠️ **不要勾选 `sandbox` profile**，它是给"无法加载 Java agent 的受限环境"用的（会排除 Mockito），正常环境不需要
4. 运行 `src/main/java/com/csu/takeout/CsuTakeoutApplication.java`
5. **不需要配任何 profile 或环境变量**：`application.yml` 的默认值就是 `localhost:3307` + `root/123456`，正好对准容器里的 MySQL

期望日志：

```
Tomcat started on port 5000 (http) with context path '/'
csu-takeout server started, API: http://localhost:5000  docs: http://localhost:5000/doc.html
```

**顺手跑测试**（56 个用例，用 H2 内存库，**不需要 MySQL**）：右键 `src/test/java` → Run 'All Tests'。

### 5.2 用 IDEA 跑前端

**必须单独开一个窗口**（一个 IDEA 窗口只支持一个 Maven/Gradle 根项目）：

1. **File → Open** → 选 `frontend\sjk` → 选 "New Window"
2. **Settings → Languages & Frameworks → Node.js**：确认 Node interpreter 指向你的 `node.exe`
3. **Alt+F12** 打开 Terminal → `npm install`（首次）
4. 右键 `package.json` → **Show npm Scripts** → 双击 **`serve`**；或直接 `npm run serve`

### 5.3 用 VS Code 跑前端（更轻）

1. **File → Open Folder** → `frontend\sjk`
2. **Ctrl+`** 打开终端 → `npm install` → `npm run serve`

---

## 第 6 章 页面验收清单

浏览器打开 **http://localhost:8080**（会自动跳到 `/login`）。

### 6.1 登录页

| 操作 | 期望 |
|---|---|
| `18873190398` / `aA.460734909` → 登录 | 提示"登录成功"，跳转用户端 `/user` |
| `18682412818` / `Aa.460734909` → 登录 | 提示"登录成功"，跳转管理端 `/manage` |
| 故意输错密码 | 提示"用户名或密码错误" |

> 页面上的"注册""忘记密码"**点了没反应是正常的**：对应接口未实现（返回 404），前端也未提供入口；遵循"不臆造未实现行为"的原则，后端不提供这三个接口。

### 6.2 用户端 `/user`（用 `18873190398 / aA.460734909` 登录）

> 进入时只挂载当前页（默认「逛店铺」），因此 Network 面板里**只有 1 个** `GET /api/user/shop`；
> 其余页面是"点开才挂载、挂载后保活"，切回来不会重复请求（原实现是全部 `v-show` 常驻，进页面同时发 4 个请求）。

| 菜单 | 看到什么 | 背后接口 |
|---|---|---|
| **逛店铺** | 8 家店铺卡片（店铺名 / 单价 / 销量 / 参考营业额）+ 搜索排序 + 购物车 | `GET /api/user/shop` |
| ↳ 加入购物车 / 立即下单 | 购物车抽屉可调数量、跨店铺选择；结算弹窗填订餐方式、姓名、地址 → 按店铺分别下单 | `POST /api/user/addorder` |
| **已完成** | 已送达订单（`checked=2`）+ 累计消费摘要 | `GET /api/user/sended` |
| **配送中** | 派送中订单（含送餐员编号、电话、预计时间） | `GET /api/user/sending` |
| **待派单** | 未派发订单 + "修改" / "取消订单" | `GET /api/user/unsend` |
| ↳ 修改订单 | 改姓名/地址 → "修改成功" | `POST /api/user/unsend` |
| ↳ 取消订单 | 确认 → "删除成功"，该行消失 | `DELETE /api/user/unsend` |
| **个人信息** | 查看态显示用户名、真实姓名、性别、年龄、电话（标「只读」）、邮箱；点「修改资料」进入编辑态，可改 5 项并保存 | `GET /api/user/usermsg`、`PUT /api/user/usermsg/edit` |
| ↳ 修改密码 | 个人信息页左侧「修改密码」按钮 → 弹窗：原密码错 → "原始密码错误"；正确 → "修改成功" | `PUT /api/user/usermsg/pwd`（与 `POST /api/user/pwd_chg` 同语义） |

> ⚠️ 改完密码记得改回 `aA.460734909`，否则后续登录与对照脚本会失败。

### 6.3 管理端 `/manage`（用 `CMX` 登录）

| 菜单 | 看到什么 | 背后接口 |
|---|---|---|
| **店铺管理** | 店铺列表 + "添加店铺" + 每行"修改"/"删除" | `GET/POST/DELETE /api/manager/shop` |
| **服务员管理** | 服务员列表 + "添加服务员"（所属店铺是**下拉框**） | `GET/POST/DELETE /api/manager/server` |
| **送餐员管理** | 配送员列表 + "添加送餐员" + "解雇" | `GET/POST/DELETE /api/manager/dispatcher` |
| **物流管理 → 已完成** | 已结束物流 | `GET /api/manager/wuliu?id=1` |
| **物流管理 → 进行中** | 进行中物流 | `GET /api/manager/wuliu?id=0` |
| **订单管理 → 已完成订单** | 已送达订单（**没有**送餐员电话列） | `GET /api/manager/sended` |
| **订单管理 → 已发货订单** | 派送中订单 | `GET /api/manager/sending` |
| **订单管理 → 未发货订单** | 未派发订单 + "派发此订单"（**下拉框**选送餐员） | `GET/POST /api/manager/unsend` |

> ⚠️ **"物流管理 → 已完成"页永远是空的**，这是既有现象：数据库中没有任何接口或触发器能修改 `wuliu.ended`（永远是默认值 0）。Java 版按契约照搬，未"修复"。

### 6.4 最有说服力的一处联动（建议重点演示）

用两个浏览器窗口（一个普通、一个无痕）：

1. **窗口 1**：`YCX / aA.460734909` → 用户端 → **未发货订单**（看到订单）
2. **窗口 2**：`CMX / Aa.460734909` → 管理端 → **订单管理 → 未发货订单** → 对同一条订单点"派发此订单"，选送餐员、填"30分钟"
3. **回到窗口 1**：该订单从**未发货订单**消失，出现在**已发货订单**，并带上送餐员编号与电话

**为什么值得讲**：派发时 Java 只做了一件事 —— 向 `wuliu` 表插入一条记录。订单状态变为"派送中"完全由触发器 `wuliu_insert` 完成，而"派送中列表"来自视图 `sending_order`（三表连接 + `checked=1` 过滤）。**Java 代码里没有一句 `UPDATE oorder SET checked=1`**。

---

## 第 7 章 触发器与视图演示脚本（面试重点）

在 Navicat 查询窗口（**先确认当前库是 `caimingxuan`**）按顺序执行：

```sql
-- ① 记录下单前的销量
SELECT shop_name, m_sale_v FROM fastfood_shop WHERE shop_name = '大美鸡公煲';

-- ② 手动插入一条订单（注意：不写 checked 列，靠数据库默认值 0）
INSERT INTO oorder (shop_name, order_money, order_way, cons_phone, cons_name, cons_addre, create_time)
VALUES ('大美鸡公煲', 15, '人工订餐', '18873190398', '测试顾客', '20公寓',
        DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'));

-- ③ 销量自动 +1（触发器 order_insert_sale）
SELECT shop_name, m_sale_v FROM fastfood_shop WHERE shop_name = '大美鸡公煲';

-- ④ 订餐方式计数自动 +1（触发器 order_insert）
SELECT * FROM orderway;

-- ⑤ 取回刚插入的订单号（假设得到 4）
SELECT order_id, cons_name, checked FROM oorder WHERE cons_name = '测试顾客';

-- ⑥ 插入物流记录 = 派发订单（把上一步的 order_id 填进去）
INSERT INTO wuliu (order_id, cons_phone, disp_id, deliver_time)
VALUES (4, '18873190398', '010100', '30分钟');

-- ⑦ 关键：订单状态自动变为 1，并立刻出现在视图里（触发器 wuliu_insert）
SELECT order_id, checked FROM oorder WHERE order_id = 4;
SELECT * FROM sending_order WHERE order_id = 4;

-- ⑧ 清理测试数据（触发器会把销量与计数减回去）
DELETE FROM wuliu  WHERE order_id = 4;
DELETE FROM oorder WHERE order_id = 4;
SELECT shop_name, m_sale_v FROM fastfood_shop WHERE shop_name = '大美鸡公煲';
```

**讲解要点**：

- 第 ② 步**故意不写 `checked`**，说明新订单初始状态来自表定义的 `DEFAULT 0`；
- 第 ⑥ 步只插入 `wuliu`，第 ⑦ 步 `checked` 就变成 1 —— 这就是 `wuliu_insert` 触发器；
- 第 ⑦ 步的 `SELECT * FROM sending_order` 证明"派送中列表"是**视图算出来的**，不是另一张表；
- 第 ⑧ 步删除后销量回落，说明 `order_delete_sale` 也在工作。

---

## 第 8 章 接口契约测试

全部接口契约由 `backend-java/src/test` 下的 **102 个用例**覆盖（H2 MySQL 兼容模式，无需 MySQL / Docker）：

- 响应体**原文**断言（空格排版、末尾换行、非 ASCII 转义、空数组）
- JWT 载荷结构与短密钥签名（手工复算 HMAC 验证补零不改变结果）
- 全部接口的端到端契约（401 / 框架级 404·405·400 / 空列表 / 越权归属校验等边界）
- 异常告警与一致性巡检（降级 / 节流窗口 / 失败吞掉 / 线程池关闭后安全丢弃；一致 / 不一致 / 表缺失 / 库不可达 / 告警与恢复；定时任务配置门禁防"假绿"）

```powershell
cd "D:\Desktop\面试\数据库项目\csu_database-master"
powershell -ExecutionPolicy Bypass -File backend-java\tools\build.ps1
```

> 触发器副作用（销量增减、`checked` 置 1）无法在 H2 上表达，需在真实 MySQL 上按第 7 章的演示脚本验证。

---

## 第 9 章 可选：DataX 跨数据库迁移复现

**这一章与系统运行无关**，只在需要演示"跨数据库迁移"时执行。

### 9.1 准备目标数据库

```powershell
# MariaDB（DataX 任务里写的是 localhost:3308/testdb）
docker run -d --name mariadb-datax -p 3308:3306 `
  -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=testdb mariadb:latest

# PostgreSQL（写的是 localhost:5432/testdb）
docker run -d --name postgres-datax -p 5432:5432 `
  -e POSTGRES_PASSWORD=123456 -e POSTGRES_USER=admin -e POSTGRES_DB=testdb postgres:latest
```

### 9.2 安装 DataX

DataX 是阿里开源的独立 Java 工具（不是 Docker 镜像）：

<https://github.com/alibaba/DataX> → 下载 `datax.tar.gz` → 解压 → 通过自带的 `datax.py` 启动器执行任务。

### 9.3 执行迁移任务

`mysql2mariadb_datax_json/` 与 `mysql2postgresql_datax_json/` 里各有 8 个任务（每张表一个）：

```powershell
# 示例：迁移 user 表到 MariaDB
datax.py ..\mysql2mariadb_datax_json\m2m_user.json
# 其余 7 个同理：fastfood_shop / oorder / orderway / server / dispatcher / wuliu / user_msg
```

### 9.4 已知的两个坑

| 坑 | 说明 |
|---|---|
| **账号密码是加密的** | job JSON 里形如 `"username": "yRjwDFuoPKlqya9h9H2Amg=="`，这是 DataX 的 AES 加密值。想重跑需用 DataX 自带的加解密工具换成明文（或直接改成 `root` / `123456` 重建 job 文件） |
| **路径是旧的** | JSON 里记的容器路径是 `D:\Desktop\Csu-Database-course-design\...`（项目改名前的），不影响运行，只影响对照 |

### 9.5 迁移结果存档

已经迁移好的结果就在仓库里，可以直接查看，无需重跑：

- `other_database/MariaDB.sql`
- `other_database/PostgreSQL.sql`（用 plpgsql 函数 + 触发器**复刻**了 MySQL 的触发器逻辑）

### 9.6 迁移后一致性校验与告警（可选）

后端内置两条检测链路，默认关闭：

| 机制 | 检测对象 | 触发方式 |
| --- | --- | --- |
| 运行期异常告警 | 未预期的系统异常（全局异常处理器兜底分支） | 请求触发（异常类级别 10 分钟节流） |
| 数据一致性巡检 | 源库 MySQL 与目标库 MariaDB / PostgreSQL 各表行数 | 定时任务 + 手动接口 |

启用（环境变量示例，对 `docker compose` 与本地 IDEA 均生效）：

```powershell
$env:MAIL_HOST="smtp.qq.com"; $env:MAIL_PORT="465"
$env:MAIL_USERNAME="你的邮箱@qq.com"; $env:MAIL_PASSWORD="SMTP授权码"
$env:ALERT_ENABLED="true"; $env:ALERT_TO="接收告警的邮箱@example.com"
$env:CONSISTENCY_ENABLED="true"     # 还要开定时巡检时才需要
```

先做一次迁移（9.3），让 `testdb` 里有数据，然后验证：

```powershell
# 1. 登录拿 token（管理端账号，见附录 A）
curl.exe -s -X POST http://127.0.0.1:5000/api/user/login -H "Content-Type: application/json" -d "{\"telephone\":\"18682412818\",\"password\":\"Aa.460734909\"}"
# 2. 测试邮件链路：收到测试邮件 = SMTP 配置成功
curl.exe -s -X POST http://127.0.0.1:5000/api/manager/alert/test -H "token: <上一步的 token>"
# 3. 手动巡检：对比源库与 MariaDB / PostgreSQL 行数
curl.exe -s -X POST http://127.0.0.1:5000/api/manager/alert/patrol -H "token: <上一步的 token>"
```

巡检返回 `consistent: true` 表示数据一致；不一致 / 表缺失 / 目标库不可达会逐表列出，并（若启用）发告警邮件；
由异常恢复一致时会发"已恢复"邮件。**不配 SMTP 也能用**：巡检照常执行，只是 `alert_sent: false`，只写日志。

常见现象对应：

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `alert_sent: false` 且日志出现"未启用 / 无收件人" | 告警未开启 | 设置 `ALERT_ENABLED=true` 与 `ALERT_TO` |
| `/alert/test` 返回 1000 | SMTP 未配置或发送失败 | 检查 `MAIL_*` 与授权码；QQ 邮箱用授权码而非登录密码 |
| 巡检报"库不可达" | 目标库没启动（MariaDB 3308 / PostgreSQL 5432 默认不开） | 按 9.1 启动对应容器，或把巡检当作"目标库存活探测"使用 |
| 一致但行数都为 0 | 目标库是空的，没迁移过 | 先按 9.3 执行迁移 |

---

## 第 10 章 停止 / 重启 / 彻底重置

```powershell
cd "D:\Desktop\面试\数据库项目\csu_database-master"
$compose = "docker compose -p csu_db -f backend-java/docker-compose.yml"

# 停止（保留数据）
docker compose -p csu_db -f backend-java/docker-compose.yml stop

# 启动已存在的容器（不重新构建，最快）
docker compose -p csu_db -f backend-java/docker-compose.yml start

# 重启
docker compose -p csu_db -f backend-java/docker-compose.yml restart

# 删除容器与网络（数据卷保留 → 数据不丢）
docker compose -p csu_db -f backend-java/docker-compose.yml down

# ⚠️ 彻底重置：连数据一起删除（下次启动会重新执行 init.sql）
docker compose -p csu_db -f backend-java/docker-compose.yml down -v
docker volume rm heu_db_mysql_data
```

**前端**：在运行 `npm run serve` 的窗口按 **Ctrl+C**，或直接关掉窗口。

**改了 Java 代码后重新构建后端**：

```powershell
docker compose -p csu_db -f backend-java/docker-compose.yml up -d --build backend
```

---

## 第 11 章 排查表（实测踩过的所有坑）

### 11.1 环境类

| 现象 | 原因 | 解决 |
|---|---|---|
| `docker : 无法将"docker"项识别为...` | Docker Desktop 未安装或未启动 | 安装并启动 Docker Desktop；或把它的 `resources\bin` 加入 PATH |
| `failed to connect to the docker API at npipe:...dockerDesktopLinuxEngine` | Docker Desktop 没启动（CLI 装了但引擎没跑） | 启动 Docker Desktop，等鲸鱼图标静止 |
| `docker version` 只打印 Client 不打印 Server | 同上 | 同上 |
| `npm -v` 报 `UnauthorizedAccess` / 禁止运行脚本 | PowerShell 执行策略为 Restricted（`npm` 走 `npm.ps1`） | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`，或改用 `npm.cmd` |
| `node : 无法将"node"项识别为...` | Node 未安装，或便携版没加入 PATH | 重装并勾选 "Add to PATH"，或手动把解压目录加入 PATH；**改完要重开终端/IDE** |
| IDEA 里找不到 Node interpreter | 装完 Node 没重启 IDE | 完全退出 IDEA 再打开 |

### 11.2 前端类

| 现象 | 原因 | 解决 |
|---|---|---|
| `npm install` 报 `ERESOLVE unable to resolve dependency tree` | npm 7+ 的 peer 依赖解析更严格 | 项目已带 `.npmrc`（`legacy-peer-deps=true`）；若仍报错：`npm install --legacy-peer-deps` |
| `npm run serve` 报 `error:0308010C:digital envelope routines::unsupported` | Node ≥17 的 OpenSSL 变化（webpack 4 时代的老问题） | 换 Node 18，或 `$env:NODE_OPTIONS="--openssl-legacy-provider"` 后重跑 |
| `Cannot find module 'less'` / `The Less implementation "undefined" not found` | **本项目原有的缺陷**：`package.json` 声明了 `less-loader` 但漏了 peer 依赖 `less` | 已修复：`MyLogReg.vue` 的 `<style lang="less" scoped>` 改为 `<style scoped>`（其样式本就是纯 CSS），并将 style 块内唯一的 `//` 注释改为 `/* */`。**无需再装 less** |
| `Syntax Error: SyntaxError`（指向 `...&lang=css&`） | 同上，style 块里有 `//` 行内注释（less 独有语法，标准 CSS 不支持） | 同上（已修复） |
| 页面能开但列表全空、Network 全红 | 后端没起 / 端口不对 / 跨域 | 先确认 `http://localhost:5000/api/user/shop` 能返回 JSON；前端 baseURL 写死 `127.0.0.1:5000` |
| 控制台报 `Network Error` | 后端未启动 | 同上 |
| 8080 端口被占用 | 上次的 dev server 没关 | 关掉旧窗口；或改 `vue.config.js` 的 `devServer.port` |
| 编译输出大量 `npm warn deprecated` | 依赖版本较老（2023 年的依赖树） | **忽略**，不影响运行 |

### 11.3 后端 / Docker 类

| 现象 | 原因 | 解决 |
|---|---|---|
| `up --build` 卡在构建很久 | 首次要下载基础镜像 + 容器内跑 `mvn package` | 正常，耐心等；或先在本机 `mvn package` 再改 Dockerfile 直接 COPY jar |
| `MySQL did not become healthy in time` | MySQL 首次初始化较慢，或镜像启动失败 | `docker compose -p csu_db -f backend-java/docker-compose.yml logs mysql` 看日志 |
| 登录提示"用户名或密码错误" | 库里的账号还是 `iu`/`mty`，第 2 步没执行成功 | 重跑 `tools\seed_test_accounts.sql`，再用 `SELECT id,username,role FROM user` 确认 |
| IDEA 启动报 `Port 5000 was already in use` | Docker 里的 backend 容器也占着 5000 | `docker compose -p csu_db -f backend-java/docker-compose.yml stop backend` |
| 后端启动报 `Communications link failure` / 连不上数据库 | MySQL 容器没起，或端口不对 | 确认 `docker ps` 里 mysql 是 `healthy`；IDEA 里默认走 `localhost:3307` |
| 删除店铺后 `server` 表里留下指向它的记录 | `server` 表**没有**指向 `fastfood_shop` 的外键，删除不会被引用阻止 | **既有遗留行为**，未做修改——修它等于改变接口语义。删除前请先解雇该店铺的服务员，避免留下孤儿行 |
| `物流管理 → 已完成` 页一直为空 | 无任何接口/触发器能修改 `wuliu.ended` | **既有现象**，按契约保留 |

### 11.4 Navicat 类

| 现象 | 原因 | 解决 |
|---|---|---|
| 连接超时 | 端口填了 3306 | 改成 **3307**（容器内才是 3306） |
| 连上但看不到 `caimingxuan` 库 | 数据卷非空，`init.sql` 未执行 | Navicat → 运行 SQL 文件 → 选 `database\caimingxuan.sql` |
| 中文显示为乱码 | 连接字符集 | 连接属性 → 高级 → 编码选 `utf8mb4` / `65001 (UTF-8)` |
| `Authentication plugin 'caching_sha2_password' cannot be loaded` | Navicat 驱动较旧 | 升级 Navicat 的 MySQL 驱动，或改用 DataGrip |

### 11.5 已修复问题备查（若你在旧副本上遇到，可对照此表）

以下问题在本仓库当前代码中**均已修复**，列出是为了让你在新环境/旧副本上遇到时能立刻识别。

| 现象 | 根因 | 修复 |
|---|---|---|
| 后端容器启动后**固定空等约 120 秒**才拉起 Java，日志却打印 `MySQL is ready` | `docker/wait-for-mysql.sh` 用了 `#!/bin/sh` + `/dev/tcp`，而 jammy 镜像的 `/bin/sh` 是 dash，**不支持 `/dev/tcp`** → 探测永远失败，只能靠超时兜底，且超时后仍误报 ready。叠加后果：Dockerfile 的 HEALTHCHECK 窗口正好也是 120s，容器可能被标记 `unhealthy` | 改为 `#!/bin/bash` + `timeout 2 bash -c "exec 3<>/dev/tcp/host/port"`；超时时输出明确的 `WARNING: ... not reachable after Ns`，不再误报。实测：对端只接受连接、不发数据也不关闭时，**153 ms 即返回成功**（详见下方"探测方式的选择"） |
| 带首尾空格的账号密码登录失败 | 接口需要对入参去首尾空白（`.strip()` 语义） | `UserServiceImpl` 对手机号与密码做 `strip()`（`null` 安全）；`UserLoginApiTest#loginTrimsWhitespace` 锁定该行为 |
| 「重复主键/唯一键」的错误提示分支实际不可达 | MyBatis-Spring 会把 `SQLException` 翻译成 Spring 的 `DataAccessException` 家族（`DuplicateKeyException` / `DataIntegrityViolationException`），早期只捕获了原始的 `SQLIntegrityConstraintViolationException`，该 handler 永远不会命中 | 改为捕获 `DuplicateKeyException` 与 `DataIntegrityViolationException`，从异常信息中提取 `Duplicate entry 'xxx'` 的冲突值拼成提示；原始 JDBC 异常保留为兜底 |
| `mvn` 构建耗时偏长 | `.dockerignore` 未排除 `src/test`（`-DskipTests` 只是不运行测试，**仍会编译**） | `.dockerignore` 增加 `src/test/`（若将来要在容器内跑测试，删掉该行即可） |
| `spring.mvc.*` 等 Boot MVC 配置不生效 | `WebMvcConfiguration` 继承了 `WebMvcConfigurationSupport`，会**关闭 Boot 的 Web MVC 自动配置** | 改为 `implements WebMvcConfigurer`；`/doc.html`、`/webjars/**`、拦截器、消息转换器均已实机复验正常 |
| `spring.main.allow-circular-references: true` 无必要 | 沿用了 sky-take-out 的配置，但本项目不存在循环依赖 | 已移除；56 个测试与启动验证均通过 |

> ⚠️ **关于"删除被 `server` 引用的店铺"，这里的行为在最近一轮已澄清（注意别按旧结论理解）**：
> - **生产库没有这个外键**。实测 `information_schema.key_column_usage`，全库只有
>   `user_msg.id → user.id` 一条外键，`server` 表上没有任何 FOREIGN KEY。
> - 因此删除被引用的店铺会**成功**（返回 `{"status": 200, "msg": "删除成功"}`），
>   并在 `server` 里留下指向不存在店铺的**孤儿行**。
> - 测试 `ManagerApiMutationTest#deleteShop` 现在锁定的正是这个行为：自己造一对
>   「店铺 + 引用它的服务员」→ 删除店铺 → 断言删除成功、`shop_range` 里不再有它、
>   而服务员行仍在（即孤儿行确实产生）。用完即清理，不碰夹具数据。
> - **曾经的说法（"数据库拒绝删除、返回未知错误、测试刻意锁定"）是错的**，源头是 H2
>   测试夹具自造了生产上并不存在的外键，让测试锁住了一个假行为。夹具已与生产对齐。
> - 这属既有遗留缺陷（删除只按 `shop_name`，不做引用检查），
>   **本次改造未修**——修它需要新增"删除前检查引用"的逻辑，会改变既有接口语义。

#### 探测方式的选择（为什么不用 `curl telnet://`）

曾用过 `curl --connect-timeout 2 telnet://host:port`，但实测其语义**不是"连上即返回"**：

| 场景 | `curl telnet://` 实测 | `exec 3<>/dev/tcp` 实测 |
|---|---|---|
| 端口拒绝 | 约 2s 退出（符合预期） | 快速返回非 0 ✅ |
| 连上但对端不发数据、不关闭 | **挂到对端关闭**（实测 25.1s） | **153 ms 返回 0** ✅ |
| 连上且对端立即发数据（如 MySQL 握手包） | 同样挂到对端关闭 | 153 ms 返回 0 ✅ |

实际后果：用 curl 时，连上 MySQL 后仍要白等到 MySQL 按 `connect_timeout`（默认 10s）踢掉连接，
每次启动多等约 10 秒；若遇到"接受 TCP 但不关闭"的防火墙/代理，容器会一直卡住起不来。

> 该结论由 `backend-java/tools/verify-wait-probe.sh` 实测得出：
> 在本机起一个"只接受连接、不发送数据、不主动关闭"的 TCP 监听，再分别用两种方式探测并计时。

---

## 附录 A 端口与账号速查

| 服务 | 地址 | 说明 |
|---|---|---|
| Vue 前端 | <http://localhost:8080> | `npm run serve` |
| 后端 API | <http://localhost:5000> | Spring Boot |
| 接口文档（Knife4j） | <http://localhost:5000/doc.html> | 含用户端 / 管理端两组 |
| OpenAPI JSON | <http://localhost:5000/v3/api-docs/swagger-config> | |
| MySQL | `localhost:3307` | 容器内 3306；库名 `caimingxuan` |
| Redis | `localhost:6379` | 当前无接口依赖 |

| 账号 | 密码 | 角色 | 进入 |
|---|---|---|---|
| `CMX` | `Aa.460734909` | role=1 | 管理端 `/manage` |
| `YCX` | `aA.460734909` | role=0 | 用户端 `/user` |
| MySQL root | `123456` | | Navicat / 命令行 |
| 手机号（CMX / YCX） | `18682412818` / `18873190398` | | 登录接口按手机号查询 |

---

## 附录 B 环境实测记录

以下为在本机（Windows + PowerShell 5.1）实测通过的环境组合，可作为排错基准：

| 项 | 实测版本 / 结果 |
|---|---|
| Docker CLI | 29.8.1 |
| Docker Compose | v5.5.1 |
| Docker Engine | ServerVersion 29.8.1，Driver `overlayfs`，`Docker Desktop (containerized)` |
| Node.js | v24.21.0（便携版，位于 `D:\develop\node-v24.21.0`） |
| npm | 11.19.0 |
| PowerShell 执行策略 | 全部 Undefined（= Restricted）→ **必须**设 `RemoteSigned` 或改用 `npm.cmd` |
| `npm install` | ✅ 成功，950 个包，约 12 秒，无错误（仅 deprecated 警告） |
| `npm run serve` | ✅ `Compiled successfully`，`App running at http://localhost:8080/` |
| 编译产物校验 | `app.js` 中含「中南外卖平台」「login_form」「userortel」，**无 less 相关错误** |
| 后端构建 | `mvn clean package` = BUILD SUCCESS，56 个测试全绿，产出 `csu-takeout.jar`（50.1 MB） |
| `node_modules` 体积 | 25135 个文件 / 约 120 MB（已在 `frontend/sjk/.gitignore` 中忽略） |

**关于 Node 24 的说明**：本项目的依赖树里**没有任何原生模块**（无 node-sass / fibers / sharp / node-gyp），且已是 webpack 5 + vue-loader 17，因此 Node 24 可正常构建运行。依赖中 `@achrinza/node-ipc@9.2.6` 的 `engines.node` 只声明到 19，属**软警告**，已通过 `.npmrc` 的 `engine-strict=false` 规避；若未来遇到它的运行时报错，改用 Node 18 即可。
