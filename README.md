# CSU 数据库课程设计 —— 校园快餐订餐管理系统

一个基于 **MySQL + Flask + Vue** 的校园快餐订餐管理系统，实现用户订餐、订单流转、配送管理等功能，并演示了视图、触发器、外键等数据库特性，以及使用 DataX 将 MySQL 数据迁移至 MariaDB / PostgreSQL 的跨数据库实践。

> **本项目现已提供 Java（Spring Boot 3）后端**，与原 Flask 后端并存、接口契约完全兼容，原 Vue 前端一行不改即可切换。详见下方「[Java 版后端](#java-版后端spring-boot-3)」章节。

## 功能特性

**用户端**
- 登录 / 注册（JWT 签发 token，Redis 存储短信验证码，短信发送代码暂时注释）
- 浏览快餐店铺列表、下单订餐
- 订单管理：未派送（可修改收货信息 / 删除订单）、派送中、已送达
- 个人信息查看与编辑（用户名 / 真实姓名 / 性别 / 年龄 / 邮箱；联系电话为查询键，只读）、页内修改密码

**管理端**
- 店铺管理：快餐店铺的增删改查（价格、销量）
- 服务员管理：服务员与所属店铺的增删
- 配送员管理：配送员的增删
- 订单管理：未派发订单的查询与派发、派送中订单、已送达订单
- 物流管理：未结束 / 已结束配送记录查询

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 2.6、Vue Router、Element UI、Axios |
| 后端（原） | Python 3.10、Flask 2.3、Flask-SQLAlchemy、PyMySQL、PyJWT、Redis |
| 后端（新） | JDK 17、Spring Boot 3.4.4、MyBatis、Druid、PageHelper、Redis、Knife4j、JJWT |
| 数据库 | MySQL 8.0（Docker 部署），含视图、触发器、外键 |
| 部署 | Docker Compose（MySQL + Redis + Backend） |
| 数据迁移 | DataX（MySQL → MariaDB / PostgreSQL） |

## 项目结构

```
csu_database-master/
├── backend/                          # Flask 后端（保留作对照实现，响应与 Java 版逐字节一致）
│   ├── app.py                        # 全部路由与业务逻辑（约 30 个接口）
│   ├── auth.py                       # JWT 生成与解析（PyJWT / HS256 / issuer=wufang）
│   ├── config.py                     # 数据库 / Redis 配置（支持环境变量覆盖）
│   ├── db/init.sql                   # 容器初始化：8 张表 + 2 个视图 + 6 个触发器 + 示例数据
│   ├── Dockerfile                    # python:3.10-slim 镜像
│   ├── docker-compose.yml            # MySQL + Redis + Flask 后端编排
│   ├── wait-for-mysql.sh             # 等待 MySQL 就绪后启动服务
│   ├── requirements.txt              # Python 依赖（Flask 2.3 / SQLAlchemy / PyMySQL / PyJWT / redis）
│   └── main.py                       # PyCharm 自动生成的示例文件，未使用（可删）
├── backend-java/                     # Java 后端（Spring Boot 3.4.4，主后端；与 Flask 版逐字节兼容）
│   ├── src/main/java/com/csu/takeout/
│   │   ├── controller/user、manager/ # 接口层：登录 / 订单 / 个人信息；店铺 / 服务员 / 配送员 / 物流 / 数据统计
│   │   ├── service(+impl) / mapper/  # 业务层与 MyBatis 持久层（SQL 全部在 resources/mapper/*.xml）
│   │   ├── json/                     # Flask 兼容 JSON 序列化器（空格排版、末尾换行、非 ASCII 转义）
│   │   ├── interceptor/ context/     # JWT 拦截器（请求头 token）+ ThreadLocal 当前用户
│   │   └── entity / dto / vo / result / handler / config / constant / utils
│   ├── src/main/resources/           # application.yml（默认 localhost:3307）+ mapper XML
│   ├── src/test/                     # 82 个接口契约测试（H2 MySQL 兼容模式，无需 Docker）
│   ├── tools/build.ps1               # 一键构建（用可写 Maven 仓库副本，受限环境可跑）
│   ├── tools/verify-wait-probe.sh    # 验证「端口就绪探测」行为的辅助脚本
│   ├── Dockerfile                    # 多阶段构建（maven → JRE 17）
│   ├── docker-compose.yml            # MySQL 3307 / Redis 6379 / API 5000
│   └── docker/wait-for-mysql.sh      # 容器内等待 MySQL 就绪（bash /dev/tcp + timeout）
├── frontend/sjk/                     # Vue 2.6 + Element UI + ECharts 5 前端
│   ├── src/api/index.js              # Axios 封装（自动携带 token）+ statsApi（看板 8 个接口）
│   ├── src/router/index.js           # 路由 /login /user /manage；meta.title 驱动标签页标题
│   ├── src/app.js                    # 应用装配（Element UI、主题、Vue.prototype.$axios）
│   ├── src/assets/styles/theme.css   # 设计 token（配色 / 圆角 / 断点）
│   ├── src/charts/echarts.js         # ECharts 按需注册 + campus-amber 主题 + 数字格式化
│   ├── src/mixins/listPager.js       # 列表页「关键字筛选 + 前端分页」混入（接口仍全量返回）
│   ├── src/utils/format.js           # 展示格式化 + ORDER_STATUS 状态文案/配色（唯一事实来源）
│   ├── src/components/common/        # AppShell / PagePanel / ChartCard / BaseChart / StatusTag
│   ├── src/components/ManageStats/   # 数据看板 8 个面板（KPI、双轴趋势、状态环、店铺榜+下钻…）
│   ├── src/components/               # MyLogReg / MyUser / MyManage / UserShop / 各订单与门店管理页
│   ├── scripts/check-theme-tokens.js # 校验 CSS 变量定义与引用一致（防模板引用了不存在的变量）
│   └── vue.config.js / package.json
├── docs/                             # 项目文档
│   ├── RUNBOOK.md                    # 从零运行手册（环境 / 启动 / 验收 / 演示 / 排查表）
│   ├── DESIGN-STATS-DASHBOARD.md     # 数据看板设计说明（口径、组件划分、交互）
│   └── preview/                      # 11 张页面截图（README 与文档引用）
├── tools/                            # 根级验收与数据脚本
│   ├── compare_api.ps1               # Flask ↔ Spring Boot 逐接口响应对照脚本
│   ├── seed_test_accounts.sql        # 幂等补齐测试账号 CMX / YCX
│   ├── gen_seed_demo_stats.ps1       # 生成「统计看板演示数据」SQL 的脚本（近 N 天批量订单）
│   └── seed_demo_stats.sql           # 生成结果（纯 INSERT + UPDATE，可直接执行）
├── database/caimingxuan.sql          # 数据库完整导出（表、视图、触发器、数据）
├── other_database/                   # 迁移后的 MariaDB / PostgreSQL 导出（PG 版含 plpgsql 触发器）
├── mysql2mariadb_datax_json/         # MySQL → MariaDB 的 DataX 任务配置（8 张表，凭据加密）
├── mysql2postgresql_datax_json/      # MySQL → PostgreSQL 的 DataX 任务配置（8 张表）
├── docker Containers/                # 各数据库容器的 docker run 命令记录（MySQL/Redis/MariaDB/PG）
├── ppt&labreport/                    # 课程设计实验报告与汇报 PPT
├── start_project.bat                 # Windows 一键启动（建卷 → 起容器 → 补账号 → 起前端）
└── .gitignore                        # 忽略 node_modules / target / dist / 日志 / .idea
```

## 快速开始

> 📖 **完整的从零运行手册见 [`docs/RUNBOOK.md`](docs/RUNBOOK.md)**：环境准备、启动、Navicat 看库、
> IDEA 调试、页面验收清单、触发器演示脚本、DataX 迁移复现、以及一份实测过的排查表。

### 环境要求

- Docker Desktop（Windows / macOS / Linux）—— 提供 MySQL 8.0 与 Redis
- Node.js 18+（前端本地运行；实测 v24.21.0 可用）
- 可选：JDK 17 + Maven（想在 IDEA 里直接跑后端时需要）
- 可选：DataX（仅跨数据库迁移演示需要）

> ⚠️ 两个容易踩的点（详见 RUNBOOK 第 1 章）：
> 1. Node 必须是**完整安装**且加入 PATH（便携版解压后要手动加 PATH，改完需重开终端/IDE）；
> 2. `npm` 走的是 `npm.ps1`，若 PowerShell 执行策略为 Restricted 会报 `UnauthorizedAccess`，
>    此时执行 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`，或改用 `npm.cmd`。

### 一键启动（Windows）

确认 Docker Desktop 已启动后，双击运行根目录下的：

```bat
start_project.bat
```

脚本会自动完成：检查 Docker → 创建外部数据卷（首次）→ `docker compose -p csu_db up -d --build` 启动 MySQL / Redis / **Java 后端** → 等待 MySQL 健康 → 补齐测试账号 → 启动 Vue 前端。

> 注意：`docker-compose.yml` 中 MySQL 数据卷 `heu_db_mysql_data` 声明为 external，首次运行前请先创建：
> ```bash
> docker volume create heu_db_mysql_data
> ```
> （`start_project.bat` 会自动创建，手动启动时才需要自己执行。）

### 手动启动

```bash
# 1. 启动后端与数据库（MySQL: 3307, Redis: 6379, API: 5000）
docker volume create heu_db_mysql_data   # 仅首次
docker compose -p csu_db -f backend-java/docker-compose.yml up -d --build

# 2. 补齐测试账号（init.sql 中的示例账号是 iu/mty，与 README 的测试账号不同）
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools/seed_test_accounts.sql

# 3. 启动前端（http://localhost:8080）
cd frontend/sjk
npm install
npm run serve
```

访问地址：前端 http://localhost:8080 ，后端 API http://localhost:5000 ，接口文档 http://localhost:5000/doc.html 。

> 想运行原 Flask 后端时，改用 `cd backend && docker compose -p csu_db up -d`（端口与数据卷完全相同，两者不会互相干扰，但同一时间只能起一个占用 5000 端口的容器）。

## 数据库设计

库名 `caimingxuan`，共 8 张表：

| 表名 | 说明 |
| --- | --- |
| `user` | 用户（id、用户名、密码、手机号、角色 role） |
| `user_msg` | 用户详细信息（外键关联 `user.id`，级联删除/更新） |
| `fastfood_shop` | 快餐店铺（名称、价格、销量） |
| `oorder` | 订单（店铺、金额、订餐方式、收货信息、状态 checked、下单时间） |
| `orderway` | 订餐方式及数量统计 |
| `server` | 服务员（编号、姓名、所属店铺） |
| `dispatcher` | 配送员（编号、姓名、电话） |
| `wuliu` | 物流配送记录（订单号、配送员、送达时间、是否结束） |

- **视图**：`sending_order`（checked = 1，派送中订单）、`sended_order`（checked = 2，已送达订单），由 `oorder`、`wuliu`、`dispatcher` 三表连接而成。
- **触发器**：
  - `order_insert` / `order_update` / `order_delete`：自动维护 `orderway.count` 统计；
  - `order_insert_sale` / `order_delete_sale`：自动增减店铺销量 `m_sale_v`；
  - `wuliu_insert`：派发物流后自动将订单 `checked` 置为 1。
- 订单状态 `checked`：`0` 未派发 → `1` 派送中 → `2` 已送达。

## 主要 API

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| POST | `/api/user/login` | 登录并返回 JWT token 与角色 |
| GET | `/api/user/shop` | 获取店铺列表 |
| POST | `/api/user/addorder` | 下单 |
| GET/POST/DELETE | `/api/user/unsend` | 未派送订单：查询 / 修改 / 删除 |
| GET | `/api/user/sending` | 派送中订单 |
| GET | `/api/user/sended` | 已送达订单 |
| GET | `/api/user/usermsg` | 个人信息 |
| POST | `/api/user/pwd_chg` | 修改密码（前端入口已并入个人信息页弹窗，走同语义的 `PUT /api/user/usermsg/pwd`；本接口保留并由契约测试覆盖） |
| GET/POST/DELETE | `/api/manager/shop` | 店铺管理 |
| GET/POST/DELETE | `/api/manager/server` | 服务员管理 |
| GET/POST/DELETE | `/api/manager/dispatcher` | 配送员管理 |
| GET/POST | `/api/manager/unsend` | 未派发订单查询与派发 |
| GET | `/api/manager/wuliu` | 物流记录（按 ended 筛选） |
| GET | `/api/manager/sending` `/api/manager/sended` | 派送中 / 已送达订单 |

除登录外，需要认证的接口通过请求头 `token` 传递 JWT，由 `backend/auth.py` 解析获取用户手机号。

## 跨数据库数据迁移（DataX）

项目使用 DataX 将 MySQL（`localhost:3307/caimingxuan`）中的数据迁移至：

- **MariaDB**（`localhost:3308/testdb`）：`mysql2mariadb_datax_json/`，读者为 `mysqlreader`，写入为 `mysqlwriter`，`writeMode: replace` 并先 `DELETE FROM`。
- **PostgreSQL**（`localhost:5432/testdb`）：`mysql2postgresql_datax_json/`，写入为 `postgresqlwriter`，迁移前 `truncate table`。

每张表一个 JSON 任务（user、user_msg、fastfood_shop、oorder、orderway、server、dispatcher、wuliu），数据库账号密码已加密存储。迁移结果见 `other_database/MariaDB.sql` 与 `other_database/PostgreSQL.sql`（PostgreSQL 版本使用 plpgsql 函数 + 触发器复刻了 MySQL 的触发器逻辑）。

> **这两个数据库只在做跨库迁移演示时才需要启动**，与前后端运行无关。系统运行时只依赖 MySQL 8.0 与 Redis。

## 说明

- 数据库连接信息可通过环境变量覆盖（`DB_HOST`、`DB_PORT`、`DB_USER`、`DB_PASSWORD`、`DB_NAME`、`REDIS_HOST`、`REDIS_PORT`），默认值见 `backend/config.py`。
- `database/caimingxuan.sql` 为完整结构 + 示例数据（含 CMX / YCX 账号），`backend/db/init.sql` 为容器首次启动时自动执行的初始化脚本（示例账号为 iu / mty）。
- 示例账号：`CMX / Aa.460734909`（role=1，管理端）、`YCX / aA.460734909`（role=0，用户端）。若用 `init.sql` 初始化，需执行 `tools/seed_test_accounts.sql` 补齐这两个账号。
  > ⚠️ **登录时填的是「手机号 + 密码」，不是用户名**：后端 SQL 是 `where telephone = ? and password = ?`
  > （见 `backend-java/src/main/resources/mapper/UserMapper.xml`），`username` 字段不参与登录。
  > 因此实际要填 **`18682412818 / Aa.460734909`**（管理端）、**`18873190398 / aA.460734909`**（用户端）；
  > 填 `CMX` / `YCX` 会一直返回"用户名或密码错误"。登录页的「演示账号」两行可以直接点击填入，避免记错。
- 课程设计文档（实验报告、汇报 PPT）位于 `ppt&labreport/` 目录。

---

# Java 版后端（Spring Boot 3）

将 `backend/`（Flask）重构为 Spring Boot 3 的版本，位于 `backend-java/`。
**原 Flask 后端保留不动**，两者可分别启动（都监听 5000，同一时间只起一个），响应契约逐字节兼容。

## 技术栈与版本选择

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| Spring Boot | 3.4.4 | 全面使用 `jakarta.*`，无任何 `javax.*` |
| JDK | 17（`maven.compiler.release=17`） | 用更高版本 JDK 编译也产出 17 字节码 |
| MyBatis | mybatis-spring-boot-starter 3.0.4 | Boot 3 兼容版 |
| PageHelper | pagehelper-spring-boot-starter 2.1.0 | 本项目列表接口均为全量返回，保留依赖以对齐技术栈 |
| Druid | druid-spring-boot-3-starter 1.2.23 | Boot 3 专用 starter |
| MySQL 驱动 | `com.mysql:mysql-connector-j` | Boot 3 起 groupId 变更，版本由 Boot 管理 |
| Redis | spring-boot-starter-data-redis | 当前无接口依赖（见下文「Redis 的实际角色」） |
| 接口文档 | knife4j-openapi3-jakarta-spring-boot-starter 4.4.0 | 替代 sky-take-out 的 springfox（Boot 3 下不可用） |
| JWT | jjwt-api / impl / jackson 0.12.6 | 替代 sky-take-out 的 jjwt 0.9.1 |

> 依赖**种类**对齐 sky-take-out（自建的 Spring Boot 2.7 外卖项目），但版本全部换成 Boot 3 兼容版；
> 分层结构、命名习惯、`Result` / `BaseContext` / 拦截器 / 全局异常处理等组织方式也向其对齐。

## 架构与包结构

```
com.csu.takeout
├── CsuTakeoutApplication        启动类（@SpringBootApplication + @MapperScan）
├── config/                      WebMvcConfiguration（拦截器/CORS/静态资源/接口文档）
│                                RedisConfiguration
├── controller/
│   ├── user/                    UserController、UserShopController、
│   │                            UserOrderController、UserMsgController
│   └── manager/                 ManagerShopController、ManagerServerController、
│                                ManagerDispatcherController、
│                                ManagerOrderController、ManagerWuliuController
├── service/ (+ impl/)           业务接口与实现
├── mapper/                      MyBatis Mapper 接口（XML 在 resources/mapper/）
├── entity/                      User、UserMsg、Order、FastfoodShop、Server、Dispatcher、Wuliu
├── dto/                         请求参数与内部传输对象
├── vo/                          响应视图对象（键名即 Flask 响应键名）
├── result/                      Result（code/msg）、TableResult（status/tabledata）、
│                                DataResult（status/data）、UserLoginResult
├── json/                        FlaskJsonObjectMapper / FlaskJsonMessageConverter / FlaskJsonWriter
├── interceptor/                 JwtTokenInterceptor（token 请求头）
├── handler/                     GlobalExceptionHandler
├── exception/                   BaseException 及子类
├── constant/                    MessageConstant、JwtClaimsConstant、RedisConstant
├── properties/                  JwtProperties（csu.jwt.*）
├── context/                     BaseContext（ThreadLocal 保存当前手机号）
└── utils/                       JwtUtil
```

## 关键设计：与 Flask 响应的逐字节兼容

Flask 的 `jsonify` 输出格式与 Jackson 默认输出差异较大，为做到"原前端不改一行、响应可逐字节对比"，实现了三个组件：

| 差异点 | Flask（Werkzeug 2.3） | Jackson 默认 | 本项目处理 |
| --- | --- | --- | --- |
| 冒号/逗号后空格 | `{"status": 200, "msg": "x"}` | `{"status":200,"msg":"x"}` | `FlaskJsonObjectMapper` 内置自定义 `PrettyPrinter` |
| 响应体末尾换行 | 有一个 `\n` | 无 | 同上（在根层对象闭合时补齐） |
| 空数组 | `[]` | `[]` | 保持一致 |
| 非 ASCII 字符 | 转义为 `\uXXXX` | 原样输出 UTF-8 | 工厂特性 `ESCAPE_NON_ASCII` |
| **转义十六进制大小写** | 小写 `\u767b` | 大写 `\u767B` | **唯一已知外观差异**，语义等价，见下方说明 |
| 字段命名 | `snake_case`（`order_id`） | Java 属性名 | 全局 `SNAKE_CASE` 命名策略 + 个别字段 `@JsonProperty` |
| 请求字段命名 | `snake_case` | Java 属性名 | 命名策略**对反序列化同样生效**（实测：无注解的 `shopName` 也能被 `shop_name` 填上），但请求 DTO 仍**全部显式标注** `@JsonProperty`——显式声明是契约，不依赖策略的隐式行为 |

> **关于 `\u767b` 与 `\u767B`**：JSON 规范允许十六进制大小写混用，浏览器 / Axios / 任何解析器结果完全一致。
> 实测确认 jackson-core 2.18.3 的 `ObjectMapper._configAndWriteValue()` 会执行
> `if (!isEnabled(ESCAPE_NON_ASCII)) generator.setCharacterEscapes(null)`，
> 即"不开启该特性就无法保留自定义转义表"，而开启后走内置的大写分支，二者不可兼得。
> 这里选择**保证转义行为正确**，`tools/compare_api.ps1` 会对十六进制大小写做归一化后比对。

### 响应结构的三种形态（与 Flask 一一对应）

| 形态 | 使用接口 | 结构 |
| --- | --- | --- |
| `Result<T>` | 登录 (+ 401 响应) | `{"code": 200\|1000, "msg": "..."}` |
| `TableResult<T>` | 19 个列表/操作类接口 | `{"status": 200\|1000, "msg"?, "tabledata"?, "disp_range"?, "shop_range"?}` |
| `DataResult<T>` | 个人信息 | `{"status": 200, "data": {...}}` |

字段为 null 时通过 `@JsonInclude(NON_NULL)` 忽略，保证"不多输出任何一个键"。

## JWT 实现细节

| 项 | Flask（auth.py） | Java 版 |
| --- | --- | --- |
| 算法 | HS256（PyJWT） | HS256（JJWT 0.12.6） |
| 密钥 | 硬编码 `12asdffa2323` | `csu.jwt.secret-key`，默认同值，可用 `JWT_SECRET` 覆盖 |
| claims | `data`（含 id/username/password/telephone）、`exp`（now+1天）、`iat`（now−1天）、`iss`（`wufang`） | 完全一致（`iat` 也刻意设为"昨天"） |
| 传输 | 请求头 `token` | 同 |
| 校验范围 | 仅 6 个用户端接口 | 同（见 `WebMvcConfiguration.JWT_PATH_PATTERNS`） |
| 校验失败 | 抛异常 → 500 HTML | 拦截器直接写 401 + `{"code":1000,"msg":"用户未登录"}` |

**两个实现要点：**

1. **短密钥处理**：Flask 的密钥只有 96 位，而 JJWT 按 RFC 7518 强制 HMAC-SHA256 密钥 ≥256 位，签名与验签都会抛 `WeakKeyException`。解决方式是构造 `SecretKeySpec` 并补零到 32 字节——RFC 2104 规定 HMAC 对短密钥本就补零至分组长度，因此**实际参与 HMAC 的密钥内容不变**，两个后端签发的 token 可以互相解析。`JwtUtilTest` 中用独立手工复算 HMAC 的方式验证了这一点。
2. **拦截器不抛异常**：实测确认 `preHandle` 中抛出的异常**不会**进入 `@RestControllerAdvice`（会绕过 DispatcherServlet 的异常解析链，最终返回 200 + "未知错误"），因此改为在拦截器内显式写出 401 响应。

## 数据库职责边界（Java 侧零重复实现）

以下逻辑**全部由数据库负责**，Java 代码只做对应的 DML/查询：

| 数据库对象 | 职责 | Java 侧做法 |
| --- | --- | --- |
| 视图 `sending_order` / `sended_order` | 三表连接 + `checked` 过滤 | 直接 `select ... from sending_order / sended_order`，不写 JOIN、不做状态过滤 |
| 触发器 `wuliu_insert` | 派发物流后把 `oorder.checked` 置 1 | 只 `insert into wuliu`，**不**更新订单状态 |
| 触发器 `order_insert` / `order_update` / `order_delete` | 维护 `orderway.count` | 只对 `oorder` 增删改 |
| 触发器 `order_insert_sale` / `order_delete_sale` | 维护 `fastfood_shop.m_sale_v` | 不写销量；写库后如需最新统计值一律重新查询 |
| `oorder.checked` 默认值 0 | 新订单初始状态 | insert 语句刻意不包含该列 |

## 配置说明

沿用 sky-take-out 的 profile 模式：

- `src/main/resources/application.yml`：占位符 + 默认值（默认 `localhost:3307` / `localhost:6379`，与 compose 的端口映射一致，本地 `mvn spring-boot:run` 开箱即用）
- `src/main/resources/application-local.yml.example`：模板，复制为 `application-local.yml` 后可覆盖（该文件已被 `.gitignore` 忽略，用于存放真实密钥）
- Docker 环境通过环境变量注入，**变量名与 Flask 版完全一致**：`DB_HOST`、`DB_PORT`、`DB_USER`、`DB_PASSWORD`、`DB_NAME`、`REDIS_HOST`、`REDIS_PORT`（另加 `SERVER_PORT`、`JWT_SECRET`）

### Redis 的实际角色

Flask 版中 Redis 仅被"发送短信验证码"相关代码使用，而该部分路由在 `app.py` 中**处于注释状态**（`/api/user/register/send_sms`、`/api/user/register/test`、`/api/user/findback`），前端点"获取验证码"实际会收到 404。
因此 Java 版**同样不实现这三个接口**（不臆造行为），只保留 Redis 依赖与配置（`RedisConstant` 登记既有的 `valid_code:{手机号}` 命名与 600 秒 TTL），并保留 Redis 服务的编排。

## 接口对照表（原接口 → 新实现）

| 方法 | 原 Flask 接口（`backend/app.py`） | 新实现（`backend-java/.../com/csu/takeout/`） |
| --- | --- | --- |
| POST | `/api/user/login`（L37） | [UserController.java:44](../backend-java/src/main/java/com/csu/takeout/controller/user/UserController.java#L44) |
| GET | `/api/user/shop`（L125） | [UserShopController.java:34](../backend-java/src/main/java/com/csu/takeout/controller/user/UserShopController.java#L34) |
| POST | `/api/user/addorder`（L140） | [UserOrderController.java:50](../backend-java/src/main/java/com/csu/takeout/controller/user/UserOrderController.java#L50) |
| GET | `/api/user/unsend`（L168） | [UserOrderController.java:62](../backend-java/src/main/java/com/csu/takeout/controller/user/UserOrderController.java#L62) |
| POST | `/api/user/unsend`（L181） | [UserOrderController.java:72](../backend-java/src/main/java/com/csu/takeout/controller/user/UserOrderController.java#L72) |
| DELETE | `/api/user/unsend`（L191） | [UserOrderController.java:85](../backend-java/src/main/java/com/csu/takeout/controller/user/UserOrderController.java#L85) |
| GET | `/api/user/sending`（L198） | [UserOrderController.java:95](../backend-java/src/main/java/com/csu/takeout/controller/user/UserOrderController.java#L95) |
| GET | `/api/user/sended`（L215） | [UserOrderController.java:105](../backend-java/src/main/java/com/csu/takeout/controller/user/UserOrderController.java#L105) |
| GET | `/api/user/usermsg`（L231） | [UserMsgController.java:36](../backend-java/src/main/java/com/csu/takeout/controller/user/UserMsgController.java#L36) |
| POST | `/api/user/pwd_chg`（L243） | [UserController.java:67](../backend-java/src/main/java/com/csu/takeout/controller/user/UserController.java#L67) |
| GET | `/api/manager/shop`（L263） | [ManagerShopController.java:39](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerShopController.java#L39) |
| POST | `/api/manager/shop`（L270 / L284） | [ManagerShopController.java:55](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerShopController.java#L55)（按 `action` 分支） |
| DELETE | `/api/manager/shop`（L293） | [ManagerShopController.java:87](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerShopController.java#L87) |
| GET | `/api/manager/server`（L303） | [ManagerServerController.java:44](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerServerController.java#L44) |
| POST | `/api/manager/server`（L316） | [ManagerServerController.java:63](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerServerController.java#L63) |
| DELETE | `/api/manager/server`（L329） | [ManagerServerController.java:73](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerServerController.java#L73) |
| GET | `/api/manager/dispatcher`（L339） | [ManagerDispatcherController.java:35](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerDispatcherController.java#L35) |
| POST | `/api/manager/dispatcher`（L346） | [ManagerDispatcherController.java:48](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerDispatcherController.java#L48) |
| DELETE | `/api/manager/dispatcher`（L360） | [ManagerDispatcherController.java:59](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerDispatcherController.java#L59) |
| GET | `/api/manager/wuliu`（L367） | [ManagerWuliuController.java:36](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerWuliuController.java#L36) |
| GET | `/api/manager/unsend`（L387） | [ManagerOrderController.java:43](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerOrderController.java#L43) |
| POST | `/api/manager/unsend`（L405） | [ManagerOrderController.java:63](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerOrderController.java#L63) |
| GET | `/api/manager/sending`（L418） | [ManagerOrderController.java:74](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerOrderController.java#L74) |
| GET | `/api/manager/sended`（L432） | [ManagerOrderController.java:84](../backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerOrderController.java#L84) |
| POST | `/api/user/register/send_sms`（L59，已注释） | **不实现**，返回 **HTTP 404**（与 Flask 一致） |
| POST | `/api/user/register/test`（L100，已注释） | **不实现**，返回 **HTTP 404**（与 Flask 一致） |
| POST | `/api/user/findback`（L86，已注释） | **不实现**，返回 **HTTP 404**（前端未调用） |

## 本地构建与测试

```powershell
# 一键构建（自动使用可写的 Maven 仓库副本，受限环境下也能跑）
powershell -ExecutionPolicy Bypass -File backend-java\tools\build.ps1

# 只打包不测试
powershell -ExecutionPolicy Bypass -File backend-java\tools\build.ps1 -SkipTests
```

测试无需 MySQL / Docker：使用 H2 的 MySQL 兼容模式（`src/test/resources/testdb/`），覆盖

- 响应体**原文**断言（空格排版、末尾换行、unicode 转义、空数组）
- JWT 载荷结构与"与 PyJWT 逐字节一致"（手工复算 HMAC）
- 20 个接口的端到端契约（含 401、外键约束、空列表等边界）
- 请勿使用 `@Transactional` 做测试隔离：测试事务与内嵌容器的 HTTP 请求线程不共享连接，请求看不到未提交数据（表现为 200 + "未知错误"）

> **H2 的局限**：H2 1.4.200 的 `CREATE TRIGGER` 只支持 `AS CALL <java方法>`，无法表达 MySQL 的 `begin...end` 触发器体，因此测试库不重建触发器。**触发器副作用（销量增减、`checked` 置 1）需在真实 MySQL 上验证**，见下方验收步骤。

## 验收步骤

### 1. 契约对照（需要两个后端都能跑）

```powershell
# 前置：Flask 端在 5001 起（与 Java 版端口错开）
#   cd backend && 修改 docker-compose 端口或本地起 Flask，使旧版监听 5001
# Java 端在 5000 起
docker compose -p csu_db -f backend-java/docker-compose.yml up -d --build

powershell -ExecutionPolicy Bypass -File tools\compare_api.ps1 `
    -FlaskBase http://127.0.0.1:5001 -JavaBase http://127.0.0.1:5000

# 只比对只读接口（不动数据库）
powershell -ExecutionPolicy Bypass -File tools\compare_api.ps1 -SkipWrites
```

脚本逐接口比对 HTTP 状态码、语义 JSON、规范化后的响应体原文，结果写入 `tools/compare-results/compare-*.json`。

### 2. 触发器与真实数据库行为（需真实 MySQL）

以下行为在 H2 上无法验证，请在真实 MySQL 上按顺序确认：

| 操作 | 期望的数据库副作用 |
| --- | --- |
| 用户下单 | `fastfood_shop.m_sale_v` +1；`orderway.count` +1；`oorder.checked` = 0 |
| 用户在"未派发"页取消订单 | `m_sale_v` −1；`orderway.count` −1 |
| 管理端派发订单 | `oorder.checked` 变为 1；订单从未派发列表消失、出现在派送中列表（视图） |
| 修改订单收货信息 | 列表内容更新，`checked` 不变 |
| 删除仍被 `server` 表引用的店铺 | **删除会成功**并留下 `server` 里指向不存在店铺的孤儿行——生产库 `server` 表**没有**指向 `fastfood_shop` 的外键（全库仅 `user_msg.id → user.id` 一条），详见「已知差异」第 12 条 |

### 3. 前端直连

```powershell
cd frontend\sjk
npm install
npm run serve     # http://localhost:8080，Axios 写死指向 127.0.0.1:5000
```

用 `18682412818 / Aa.460734909` 走管理端、`18873190398 / aA.460734909` 走用户端（登录填手机号，不填 CMX / YCX），逐页面确认功能正常。

## 已知差异与说明（Java 版）

| # | 项 | 说明 |
| --- | --- | --- |
| 1 | unicode 转义十六进制大小写 | Jackson 输出大写 `\u767B`，Python 输出小写 `\u767b`；语义等价，对比脚本做了归一化 |
| 2 | 令牌缺失/非法 | Flask 返回 500 HTML，Java 版返回 401 + JSON（前端 axios 两个分支都不会崩） |
| 3 | 未派发订单为空时 `tabledata` | 均输出 `[]`，一致 |
| 4 | 数据库异常 | Flask 直接 500；Java 版收敛为 200 + `{"status":1000,"msg":"未知错误"}`，前端能正常提示 |
| 5 | `usermsg` 查不到记录 | Flask 因 `data[0]` 越界返回 500；Java 版返回 **`{"status": 200}`**（`data` 为 null 时被 `@JsonInclude(NON_NULL)` 整个省略，**不是** `"data": null`）。正常有数据时两边响应完全一致。已由 `UserLoginApiTest#userMsgWithoutRecord` 锁定 |
| 6 | 店铺 `action` 取值非法 | Flask 因无分支命中返回 500；Java 版返回业务失败响应 |
| 7 | SQL 注入 | Flask 版全部使用字符串拼接（约 20 处）；Java 版改为 MyBatis 预编译参数，行为与响应不变，属安全性改进 |
| 8 | JWT 载荷含明文密码 | 沿用原实现（`data.password`），未做改动以保证 token 兼容 |
| 9 | `wuliu.ended` 无接口可改 | 沿用原状，"已完成物流"页恒为空 |
| 10 | 框架级错误的状态码 | 本轮修复：路径不存在 → **404**、HTTP 方法不允许 → **405**（带 `Allow` 头）、查询参数类型不匹配（如 `?days=abc`）→ **400**。修复前这三类都被兜底异常处理器收成 `200 + {"status":1000,"msg":"未知错误"}`，会让反向代理/监控/调用方无法区分"接口不存在"与"业务失败"。响应体为空（不再包一层 status/msg）：这三类不属于业务契约，`4xx` 即代表调用姿势不对，判定规则保持单一。请求体解析失败（字段名写错、多传未声明字段）**仍然**是 `200 + {"status":1000,...}`——那是"打到了接口但内容不合契约"，属业务响应 |
| 11 | 改单/取消订单的归属校验 | 本轮修复：`POST/DELETE /api/user/unsend` 的 SQL 增加 `and cons_phone = #{consPhone}`（手机号取自 JWT），只能操作自己的订单。Flask 版只按 `order_id`，任意登录用户可改/删他人订单，而删除会触发 `order_delete_sale` 误减**别人店铺**的销量。一行都没影响时返回 `{"status":1000,"msg":"订单不存在或不属于当前用户"}`（两种情况合并同一句提示，避免用不同文案枚举出他人订单号）。由 `UserApiMutationTest#cannotTouchOthersOrder` 锁定 |
| 12 | 删店铺会留下 `server` 孤儿行 | 生产库 `server` 表**没有**指向 `fastfood_shop` 的外键（实测 information_schema 只有 `user_msg.id → user.id` 一条），因此删除被服务员引用的店铺会成功，并留下指向不存在店铺的孤儿行。**沿用 Flask 行为未修**（修它等于改变既有接口语义）。测试夹具原先自造了该外键，会让测试锁住一个生产上不成立的行为，已与生产对齐 |
| 13 | 单次 `data.sql` 之外 | 容器以 `init.sql` 初始化，示例账号为 `iu` / `mty`，需执行 `tools/seed_test_accounts.sql` 补齐 `CMX` / `YCX` |

## 与 sky-take-out 的对齐关系

| sky-take-out 的做法 | Java 版的处理 |
| --- | --- |
| `Result<T>`（code/msg/data）+ 静态工厂 | 保留类与组织方式，另按 Flask 契约增加 `TableResult` / `DataResult` |
| `BaseContext` ThreadLocal | 保留，泛型参数由 `Long id` 改为 `String telephone`（Flask 以手机号为检索键） |
| `JwtProperties` + `JwtUtil` + `JwtTokenUserInterceptor` + `WebMvcConfiguration` | 全部保留；请求头由 `authentication` 改为 `token`，仅保留一个拦截器 |
| `GlobalExceptionHandler` + `BaseException` + `MessageConstant` | 保留；文案换成本项目内容 |
| 分层与包结构（controller/service/mapper/entity/dto/vo/config/...） | 完全对齐（单模块，不拆 common/pojo/server） |
| `springfox` + `Docket`（Boot 2.7） | 换为 Knife4j 4.x + springdoc `GroupedOpenApi`（Boot 3 下 springfox 不可用） |
| PageHelper 1.3.0、Druid 1.2.1、jjwt 0.9.1 | 分别升级为 2.1.0、druid-spring-boot-3 1.2.23、jjwt 0.12.6 |
| `application.yml` + `application-local.yml`（忽略）+ `.gitignore` | 保留该 profile 模式 |
| `spring.jackson` 驼峰映射开启 | 本项目**关闭** `map-underscore-to-camel-case`，改用 SQL 别名 + 全局 `SNAKE_CASE` 命名策略精确控制响应键名 |

---

# 数据可视化（管理端数据看板）

原有的管理端只有 5 组 CRUD 列表：能查"某一条订单现在是什么状态"，但回答不了"最近一周卖得怎么样、谁在送、哪个时段最忙"。
本组功能把 `oorder` / `wuliu` / `orderway` 里已经流转出来的数据实时算成 8 个面板，作为管理端「总览」分组的默认首页。

| 项 | 说明 |
| --- | --- |
| 定位 | 管理端默认页：登录后 role≠0 直接进 `/manage`，`MyManage.vue` 的 `active` 初值就是 `stats` |
| 数据来源 | 现有 8 张表 + 2 个视图（`sending_order` / `sended_order`），**不新增表、不新增字段、不建索引** |
| 写操作 | 无：8 个接口全部是 GET + SELECT，不触碰任何写路径与触发器副作用 |
| 新增依赖 | 后端 0 个；前端只新增 `echarts`（按需注册，见 `src/charts/echarts.js`） |
| 与 sky-take-out 一类通用外卖后台的差异 | 不做"图表铺满一屏"的模板化报表：每个面板都在副标题或页脚写清聚合口径，并刻意把「数据库触发器维护的统计值」（`orderway.count`、`fastfood_shop.m_sale_v`）与「按 `oorder` 实时聚合的值」当成两类数据分别呈现——两者本来就不是同一个口径，混在一起看只会被误读成"数据不一致" |

## 接口对照表

8 个接口统一挂在 `ManagerStatsController`（`@RequestMapping("/api/manager/stats")`）下，成功响应为 `{"status": 200, "data": {...}}`；唯一例外是下钻接口，店铺不存在时返回 `{"status": 1000, "msg": "该店铺不存在"}`（`DataResult` 只有 status/data 两个键，其定义以 `/api/user/usermsg` 为准，不为了统计接口去改它）。

| 接口 | 路径 | 参数 | 数据来源 | 聚合思路 | 后端实现（文件:行号） |
| --- | --- | --- | --- | --- | --- |
| 经营总览 | GET `/api/manager/stats/overview` | 无 | `oorder`、`fastfood_shop`、`user`、视图 `sending_order` / `sended_order` | 一次扫 `oorder` 同时取 `count(*)` 与 `sum(order_money)`（避免两次全表扫描）；未派发直接 `count(checked = 0)`；派送中 / 已送达分别 `count(*)` 两个视图 | [ctl:56](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L56) · [xml:25](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L25) |
| 订单量与营业额走势 | GET `/api/manager/stats/trend` | `days`（默认 7，服务端收敛到 `[1, 90]`） | `oorder.create_time` | `substring(create_time, 1, 10)` 按天分组，`count(*)` 与 `sum(order_money)` 一次取回；区间内无订单的日期由服务端补 0，保证折线连续、横轴等距 | [ctl:74](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L74) · [xml:50](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L50) |
| 单店铺走势（下钻） | GET `/api/manager/stats/shop/trend` | `shop_name`、`days`（默认 7） | `fastfood_shop` + `oorder` | 先用 `getShopBase` 校验店铺是否存在（不存在或店铺名为空 → `status 1000`），再复用同一段按天聚合，SQL 里用 `<if>` 追加 `shop_name = #{}` | [ctl:90](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L90) · [xml:99](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L99) |
| 订单状态分布 | GET `/api/manager/stats/status` | 无 | `oorder.checked` | `where checked in (0,1,2) group by checked`，只统计三种有效状态；Java 侧按固定的 0/1/2 顺序补齐缺失档位并映射中文文案 | [ctl:119](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L119) · [xml:65](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L65) |
| 店铺业绩榜 | GET `/api/manager/stats/shops` | `limit`（默认 8，收敛到 `[1, 20]`）、`metric`（`money` 默认 / `count`，非法值回落 `money`） | `oorder` join `fastfood_shop` | 按 `shop_name` 聚合单量与金额，带出 `price` 与触发器维护的 `m_sale_v`；排序口径用 `<choose>` 分支而不是 `${}` 拼接（不留注入面）；名次在 Java 侧补 | [ctl:131](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L131) · [xml:78](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L78) |
| 订餐方式构成 | GET `/api/manager/stats/orderway` | 无 | `orderway` 表（`count` 由触发器维护） | `select orderway_name, count from orderway order by count desc`，**不重算也不过滤**；`ratio` 在 Java 侧按合计算，保留一位小数 | [ctl:147](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L147) · [xml:108](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L108) |
| 配送员工作量对比 | GET `/api/manager/stats/dispatchers` | 无 | `dispatcher` left join `wuliu` | 按 `disp_id` 分组，用 `case when ended = 0/1` 拆成"在途 / 已完成"两段计数；left join 保证没派过单的配送员也出现在列表里（数量 0） | [ctl:156](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L156) · [xml:119](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L119) |
| 下单时段分布 | GET `/api/manager/stats/hours` | 无 | `oorder.create_time` | `hour(create_time)` 取小时分桶，Java 侧补零到 0–23 共 24 个桶并求峰值（比较用严格大于，并列时取更早的小时，结果稳定可测） | [ctl:165](backend-java/src/main/java/com/csu/takeout/controller/manager/ManagerStatsController.java#L165) · [xml:138](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L138) |

Java 侧的三件事——**日期补零、小时补零、比率与日均计算**——都放在 `ManagerStatsServiceImpl` 里，不下沉到 SQL（SQL 里做补零要么不可移植，要么出现除零风险）。
对应行号：类级只读注解 L42、总览 L65、走势 L79、下钻 L87、状态 L106、榜单 L131、订餐方式 L148、配送员 L172、时段 L177，补零主流程 `buildTrend` L211、`buildDateRange` L245。

## 图表清单（8 个面板）

| # | 面板 | 图表类型 | 调用接口 | 前端组件 | 关键交互 |
| --- | --- | --- | --- | --- | --- |
| 1 | KPI 指标卡区（6 张卡） | 指标卡（非图表） | `/stats/overview` | [KpiCards.vue](frontend/sjk/src/components/ManageStats/KpiCards.vue)（由 `StatsDashboard.vue` 编排） | 顶部"刷新"按钮统一重取；卡片底部提示随区间切换显示"近 N 天日均"；总营业额超 1 万自动紧凑显示（`1.2万`） |
| 2 | 订单量与营业额走势 | 双 Y 轴：订单量走面积线（左轴）、营业额走折线（右轴） | `/stats/trend?days=` | [TrendChart.vue](frontend/sjk/src/components/ManageStats/TrendChart.vue) | 右上「近 7 天 / 近 30 天」切换（`el-radio-group` 的 `@change` 直接调 `load()`）；30 天时 x 轴标签自动抽稀 |
| 3 | 订单状态构成 | 环形图（中心总数用 DOM 叠层，不占 ECharts 组件） | `/stats/status` | [StatusDonut.vue](frontend/sjk/src/components/ManageStats/StatusDonut.vue) | 无筛选控件；tooltip 显示单量 + 占比；页脚算"已送达率 / 在途待派" |
| 4 | 订餐方式构成 | 饼图 | `/stats/orderway` | [OrderWayPie.vue](frontend/sjk/src/components/ManageStats/OrderWayPie.vue) | 图例带 `ratio` 百分比；页脚显式写明这是触发器维护的历史累计值 |
| 5 | 配送员工作量对比 | 堆叠柱状图（在途 / 已完成） | `/stats/dispatchers` | [DispatcherBar.vue](frontend/sjk/src/components/ManageStats/DispatcherBar.vue) | tooltip 末尾追加送达率；右上角显示"在途 / 已完成"合计；柱顶标注个人总量 |
| 6 | 下单时段分布 | 柱状图（按午市/晚市/其他分三档柱色）+ 峰值 `markLine` | `/stats/hours` | [HourBar.vue](frontend/sjk/src/components/ManageStats/HourBar.vue) | 右上 `el-tag` 显示高峰小时与单量；tooltip 标注该小时属于午市/晚市/非高峰 |
| 7 | 店铺业绩榜 | 横向条形图（Top N，前三名主色渐进） | `/stats/shops?limit=&metric=` | [ShopRankChart.vue](frontend/sjk/src/components/ManageStats/ShopRankChart.vue) | **点击柱子下钻**（`chart-click` → `$emit('drill')`）；「按营业额 / 按订单量」切换；Top 5 / 8 / 12 下拉 |
| 8 | 店铺下钻抽屉 | 双轴柱+折线 + 明细表格 | `/stats/shop/trend?shop_name=&days=` | [ShopDrillDrawer.vue](frontend/sjk/src/components/ManageStats/ShopDrillDrawer.vue) | 由榜单点柱子打开（右侧 620px `el-drawer`）；区间默认跟随看板当前选择，抽屉内可再切 7/30 天；表格按日期倒序并算单均 |

页面编排顺序（`StatsDashboard.vue`）：KPI 区 → 趋势图 → `[状态环形图 | 订餐方式饼图]` → `[配送员柱状图 | 时段柱状图]` → 店铺榜 → 下钻抽屉。

## 设计说明

主题名 **暖食琥珀 × 学院米白**（定义在 `theme.css` 头部注释）。主色取琥珀橙而非后台模板惯用的亮蓝，
侧栏用墨蓝 `#2A3B4C` 而非纯黑，页面底色用米白 `#F6F4F0` 而非冷灰——三条是同一套思路：**暖色统一色温、深色压住暖调**。

| Token | 色值 | 用途 |
| --- | --- | --- |
| `--csu-primary` | `#E8622C` | 主色（琥珀橙）：主按钮、选中态、图表第一序列 |
| `--csu-primary-dark` | `#C44E1E` | 主按钮 hover / focus |
| `--csu-primary-light` | `#F58A5C` | 图表强调色（tooltip 轴指示线、柱子 hover） |
| `--csu-primary-bg` | `#FDEDE5` | 主色浅底（表格行 hover、plain 按钮底） |
| `--csu-primary-text` | `#B8431A` | 主色浅底上的文字色（保证 WCAG AA 对比度） |
| `--csu-sidebar` | `#2A3B4C` | 侧栏 / 顶栏品牌区底色（墨蓝） |
| `--csu-sidebar-hover` | `#35495D` | 侧栏菜单 hover |
| `--csu-sidebar-text` | `#B9C6D3` | 侧栏菜单文字 |
| `--csu-sidebar-title` | `#7B8C9C` | 侧栏分组标题、侧栏页脚 |
| `--csu-topbar-bg` | `#FFFFFF` | 顶部条 |
| `--csu-bg` | `#F6F4F0` | 页面底色（学院米白） |
| `--csu-panel` | `#FFFDF9` | 卡片底色 |
| `--csu-panel-alt` | `#F7F3EC` | 次级底（下钻抽屉里的 KPI 小块） |
| `--csu-header-bg` | `#F0EBE4` | 表格表头 |
| `--csu-border` / `--csu-border-light` | `#E8E2D9` / `#F1ECE5` | 常规描边 / 细描边与网格线 |
| `--csu-text` / `--csu-text-sub` / `--csu-text-weak` | `#26323D` / `#5C6B7A` / `#9AA7B4` | 主文字 / 次要文字与坐标轴标签 / 口径说明与占位 |
| `--csu-success` | `#2FA36B` | 语义色：已完成 |
| `--csu-warning` | `#E0A020` | 语义色：待派单 |
| `--csu-danger` | `#D6453D` | 语义色：未派发（也是 KPI 卡色条） |
| `--csu-border-primary` | `#F5C4AE` | 主色浅底块（`--csu-primary-bg`）的描边 |
| `--csu-icon-empty` | `#DED6CA` | 空态/占位图标（比 `--csu-text-weak` 更淡） |
| `--csu-text-invert` | `#FFFFFF` | 深色底上的文字 |
| `--csu-text-on-dark` / `--csu-text-on-dark-dim` / `--csu-text-on-dark-body` | `#C6D2DE` / `#8C9BAC` / `#A9B7C6` | 登录页与侧栏深色区上的三级文字 |
| `--csu-sep-on-dark` | `#C9D2DA` | 深色区里的分隔符 |
| `--csu-primary-gradient-to` / `--csu-login-gradient` | `#F0904F` / 三色渐变 | logo 方块与登录页品牌区渐变 |
| `--csu-skeleton-from` / `--csu-skeleton-to` | `#F1ECE5` / `#E8E2D9` | 图表骨架屏的两个灰阶 |

> **图表配色的事实来源是 `charts/echarts.js` 的 `C` 常量**（CANVAS 渲染读不到 CSS 变量，必须传具体色值）。
> DOM 样式看本表，图表看 `echarts.js`；两处同名颜色的对应关系由 `npm run check:theme` 逐对断言
> （14 组），改动任一边不一致时会直接报错。该脚本同时检查：`var(--csu-*)` 引用都有定义、
> 没有"定义了却没人用"的死 token、组件 `<style>` 段内零硬编码色值、以及 `C` 的声明顺序
> 早于引用它的 `CHART_COLORS`（顺序颠倒会因 TDZ 导致整站白屏，已验证过）。

**与 sky-take-out 的差异点（每一条都是决策，不是换皮）：**

| # | 差异点 | 本项目做法 | 理由 |
| --- | --- | --- | --- |
| 1 | 侧栏配色 | 深色墨蓝 `#2A3B4C`，不是浅色侧栏 | 看板整屏都是浅色卡片（米白底 + 奶白卡），左侧再放一块浅色导航，页面就没有左右边界，视线会从导航直接滑进图表区；深色侧栏给内容区一个明确的左边界。用墨蓝而非纯黑，是因为纯黑压在暖色底上会显脏，墨蓝能压住暖调又不发飘 |
| 2 | 主色 | 琥珀橙 `#E8622C`，不是亮蓝 | 暖色对"吃"有食欲暗示，但比正橙 / 正红克制，长时间盯盘不刺眼；更实际的原因是**色相距离**：琥珀橙与语义色（成功 `#2FA36B`、警告 `#E0A020`、危险 `#D6453D`）拉开得够远，状态标签与主色不会互相混淆 |
| 3 | KPI 卡 | 左侧 4px 语义色条 + 右上小图标 + 等宽数字 + 底部口径说明，不用"渐变图标块 + 大数字"模板 | 6 张卡横排时，彩色图标块会把视觉重心拉到每张卡的右上角；左侧色条与卡片边线共线，扫视时形成一条整齐的色带，可以按颜色直接定位"要看哪张卡"。数字走等宽字体（`--csu-font-mono`），刷新时不会左右跳动 |
| 4 | 图表口径副标题 | `ChartCard` 的副标题与页脚必须写清"这张图算的是什么" | 看板里同时存在"触发器维护的累计值"与"按 `oorder` 实时聚合值"两种口径，不写清楚就会被当成数据不一致；例如订餐方式饼图的页脚明说它读的是 `orderway` 表的历史累计值，而相邻的状态环形图是实时聚合 |
| 5 | 状态标签与图表同色 | `StatusTag`（列表页）与状态环形图 / 配送员柱状图共用同一组语义色：待派单 `#E0A020`、配送中 `#E8622C`、已完成 `#2FA36B` | 看板上"橙色块变多"可以直接对应到订单列表里那一列橙色 tag，不需要二次记忆；也正因如此没有沿用 `el-tag` 的默认配色（其圆角与底色在密集表格里视觉噪音偏大） |

## 只读保证与鉴权

看板是纯读功能，只读性由三层共同保证：

| 保证 | 落实位置 | 说明 |
| --- | --- | --- |
| 全部只读 SQL | `ManagerStatsMapper.xml` | 13 条 `<select>`，文件里没有任何 `<insert>` / `<update>` / `<delete>` |
| 类级只读事务 | [ManagerStatsServiceImpl.java:42](backend-java/src/main/java/com/csu/takeout/service/impl/ManagerStatsServiceImpl.java#L42) | `@Transactional(readOnly = true)` 标在类上，8 个方法全部落在只读事务内 |
| 复用数据库视图 | [ManagerStatsMapper.xml:36](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L36) / [:40](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L40) | "派送中 / 已送达"直接 `select count(*) from sending_order / sended_order`，Java 侧不写 JOIN、不判 `checked`，与既有列表接口保持同一条口径 |
| `orderway.count` 只读不重算 | [ManagerStatsMapper.xml:108](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L108) | 订餐方式构成直接 `select ... from orderway`，`count` 由触发器 `order_insert` / `order_update` / `order_delete` 维护 |
| `fastfood_shop.m_sale_v` 只读不重算 | [ManagerStatsMapper.xml:83](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L83) / [:102](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L102) | 店铺榜 join 时带出 `m_sale_v`，下钻只 `select` 店铺静态信息，Java 侧从不更新销量 |

因为没有 DDL、没有 DML，统计接口**不可能触发** `wuliu_insert`、`order_insert_sale`、`order_delete_sale` 等触发器的副作用，也就不存在"看一眼报表把数据看变了"的风险。

鉴权方面，本组接口是全项目**第一批**默认需要 token 的管理端接口：

| 项 | 内容 |
| --- | --- |
| 拦截名单 | `WebMvcConfiguration.JWT_PATH_PATTERNS`（[L59-L67](backend-java/src/main/java/com/csu/takeout/config/WebMvcConfiguration.java#L59)） |
| 原有 6 条 | `/api/user/addorder`、`/api/user/unsend`、`/api/user/sending`、`/api/user/sended`、`/api/user/usermsg`、`/api/user/pwd_chg`（与 Flask 版 `get_token_phone` 的调用点一一对应） |
| 新增 1 条 | `/api/manager/stats/**`（L66）——覆盖全部 8 个统计接口，包括下钻 |
| 既有管理端接口 | 15 个映射：`shop` / `server` / `dispatcher` 各 3 个、`unsend` 2 个、`sending` / `sended` / `wuliu` 各 1 个，全部**不在名单内**，鉴权行为与 Flask 版逐字节一致，本次未做任何改动 |
| 校验失败响应 | 拦截器直接写 401 + `{"code": 1000, "msg": "用户未登录"}`（在拦截器内显式写出而不抛异常，原因见上文「JWT 实现细节」） |
| 对现有前端的影响 | **零**：`api/index.js` 的请求拦截器（L12-L20）本来就在每个请求头带 `token`，此前只是既有接口不校验而已；统计接口开始校验后，前端一行都不用改，只是"带的 token 终于被用上了" |
| 测试锁定 | `ManagerStatsApiTest#statsRequireToken`（401）与 `#existingManagerApiStillOpen`（不带 token 调 `/api/manager/shop` 仍 200） |

## 演示数据

`init.sql` / `caimingxuan.sql` 里只有 3 条订单、1 条物流记录，趋势图是一条贴地的直线、状态环形图只有一根柱子，看板看不出任何东西。为此提供了一对"生成 + 执行"的脚本：

| 文件 | 作用 |
| --- | --- |
| `tools/gen_seed_demo_stats.ps1` | 只生成 SQL 文件，不连数据库、不改结构、不含任何 DDL。参数 `-Seed`（默认 `20241216`）与 `-Days`（默认 `30`）；按"最近 N 天"生成时间分布真实的订单与物流数据，最近 7 天约 33 单/天、第 8–30 天约 5 单/天，让 7 天 / 30 天两种区间都有观感。输出固定写到 `tools/seed_demo_stats.sql` |
| `tools/seed_demo_stats.sql` | 生成结果，纯 INSERT + UPDATE，可人工审阅后再执行。头部注释写明执行顺序、影响行数与回滚方式；正文顺序为 `user_msg` → `oorder` → `wuliu` → 状态校正 → 统计列重算 |

> ⚠️ **关键坑：SQL 的执行顺序不能改。** 数据库触发器 `wuliu_insert` 会在插入 `wuliu` 记录时把对应订单的 `oorder.checked`
> 强制置为 `1`（"已派发"）。因此脚本必须是「**先插 `oorder`（`checked` 一律写 0）→ 再插 `wuliu` → 最后按 `wuliu.ended` 校正 `checked`**」：
> `ended = 1` → `checked = 2`（已送达）、`ended = 0` → `checked = 1`（在途）、无物流记录 → 保持 `0`（未派发）。
> 顺序一旦颠倒（例如把状态校正放在 `wuliu` 插入之前），触发器会把"已送达"全部改成"派送中"，看板的状态分布与已送达率同时失真，
> 而且**不会报任何错**——这是本组功能里最容易踩、也最难自查的一个坑。

影响的行数（`seed_demo_stats.sql` 头部注释）：

| 对象 | 变化 |
| --- | --- |
| `oorder` | +355 行（`order_id` 从 4 开始续编；1..3 是初始化脚本自带的示例订单） |
| `wuliu` | +293 行（非未派发的订单各一条） |
| `orderway.count` | 重算为 `oorder` 的实际行数（原本是 51/108 的历史遗留值） |
| `fastfood_shop.m_sale_v` | 重算为 `oorder` 的实际行数（等价于触发器 `order_insert_sale` 累积后的结果） |
| `user_msg` | +1 行（补齐 CMX 的明细行，否则管理端账号进用户端页面会报错） |

**生成**（需要时重新生成，默认参数即可）：

```powershell
powershell -ExecutionPolicy Bypass -File tools\gen_seed_demo_stats.ps1
powershell -ExecutionPolicy Bypass -File tools\gen_seed_demo_stats.ps1 -Seed 7 -Days 30
```

**执行**：

```bash
# Git Bash / WSL / cmd 下（与 seed_test_accounts.sql 同一写法）
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools/seed_demo_stats.sql

# 若中文显示为问号，加上字符集参数
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 --default-character-set=utf8mb4 caimingxuan < tools/seed_demo_stats.sql
```

```powershell
# PowerShell 不支持 "<" 输入重定向（会报 The '<' operator is reserved for future use），改用管道
Get-Content -Raw tools\seed_demo_stats.sql | docker exec -i csu_db-mysql-1 mysql -uroot -p123456 --default-character-set=utf8mb4 caimingxuan
```

执行后用脚本末尾注释里的三条 SQL 自查（期望：状态分布 0/1/2 三档都有量，不再是"全挤在 1"）：

```sql
SELECT checked, COUNT(*), SUM(order_money) FROM oorder GROUP BY checked;
SELECT DATE(create_time), COUNT(*), SUM(order_money) FROM oorder GROUP BY DATE(create_time) ORDER BY 1;
SELECT d.dispatcher_name, COUNT(w.order_id) FROM dispatcher d LEFT JOIN wuliu w ON w.disp_id = d.dispatcher_id GROUP BY d.dispatcher_id;
```

**回滚**（只删本脚本插入的数据，`order_id` 1..3 是初始化脚本自带的）：

```sql
DELETE FROM wuliu  WHERE order_id > 3;
DELETE FROM oorder WHERE order_id > 3;
```

随后执行 `seed_demo_stats.sql` 末尾的两条 UPDATE 重算 `orderway.count` 与 `m_sale_v`（或直接从备份恢复）。
`user_msg` 里为 CMX 补的那一行只在原本缺失时才插入，回滚时**不必删**——删掉反而会让管理端账号进用户端报错。

## 契约测试

`ManagerStatsApiTest` 跑在 H2 的 MySQL 兼容模式上，**不需要 MySQL / Docker**，与既有接口的契约测试共用同一套设施（`ApiContractTestSupport` + `testdb/schema.sql`、`testdb/data.sql`）。

| 覆盖点 | 用例 | 断言要点 |
| --- | --- | --- |
| JWT 401 | `statsRequireToken` | 遍历 `overview` / `trend` / `status` / `shops` / `orderway` / `dispatchers` / `hours` 7 个 URL，未带 token 一律 401，响应体逐字节等于 `{"code": 1000, "msg": "\u7528\u6237\u672A\u767B\u5F55"}\n`（十六进制大写，与本项目 Jackson 的输出一致） |
| 零回归 | `existingManagerApiStillOpen` | 不带 token 请求 `/api/manager/shop` 仍为 200 且含 `"status": 200` |
| 总览 | `overview` | 4 单 / 60 元 / 2 店 / 2 用户 / 未派发 2 / 派送中 1 / 已送达 1，且响应以 `}\n` 结尾 |
| 日期补零 | `trendFillsMissingDays` | 7 天窗口内只有当天有单：`order_counts: [0,0,0,0,0,0,1]`、`revenues: [0,0,0,0,0,0,20]`；日均按窗口天数分摊（2.9 元） |
| 参数收敛 | `trendClampsDays` / `shopRankLimit` / `shopRankMetric` | `days=0` 与 `-5` → 1、`days=1000` → 90；`limit=0` 只出 `rank 1`、`limit=999` 出到 `rank 2`；非法 `metric` 回落 `money` |
| 状态与文案 | `statusDistribution` | 三档齐全，`label` 为 待派单 / 配送中 / 已完成（与前端标签一致） |
| 店铺榜 | `shopRank` | 按营业额降序、`rank` 从 1 连续、带 `price` 与 `m_sale_v` |
| 下钻 | `shopTrend` | 存在 → `status 200` + `shop_name`；不存在 → `{"status": 1000, "msg": "该店铺不存在"}\n`；`shop_name` 缺省 → `status 1000` |
| 订餐方式 | `orderWay` | 直接读表：108 / 51 → `ratio` 67.9 / 32.1，`total` 159 |
| 配送员 | `dispatcherWorkload` | 有单者计数正确；没派过单的 `010101` 仍在列表里且 `total = 0` |
| 小时桶 | `hourDistribution` | 固定 24 个桶（`"hours": [` 只出现一次）、峰值 `peak_hour = 14`、`peak_count = 3` |
| 只读性 | `statsAreReadOnly` | 连续两次 `overview` 响应完全一致；统计调用后 `select count(*) from oorder` 仍为 4 |

测试隔离上踩过、并且**已在代码里留下处理痕迹**的坑：

| 坑 | 现象 | 处理 |
| --- | --- | --- |
| H2 内存库跨上下文复用 | 本类标了 `@DirtiesContext(AFTER_CLASS)`（它断言精确订单数，而同层写操作用例会新增订单污染计数），上下文重建会重跑 `schema.sql` / `data.sql`；库名固定且带 `DB_CLOSE_DELAY=-1` 时重放必然撞主键，表现为"上下文重建后数据没复位" | 库名带随机后缀 `${random.uuid}`（[application-h2test.yml:11](backend-java/src/test/resources/application-h2test.yml#L11)），每个上下文一个新库，初始化即可干净重放 |
| H2 1.4.200 没有实现 `date_format()` | 按天聚合的 SQL 在契约测试里跑不起来，等于这块逻辑没有测试覆盖 | 改用 `substring(create_time, 1, 10)`（`create_time` 是 `varchar` 且格式固定，MySQL 8 与 H2 行为一致）；范围过滤同样用字符串比较，字典序即时间序 |
| H2 把 `hour()` 当成聚合函数 | `group by` 只写别名会报 "Column create_time must be in the GROUP BY list" | `group by` 里重复整个表达式 `group by cast(hour(create_time) as char)`（[xml:143](backend-java/src/main/resources/mapper/ManagerStatsMapper.xml#L143) 留了注释） |
| MyBatis 用 `Map` 接聚合结果 | H2 下聚合列没被映射进来，`get("totalOrders")` 为 null，总订单数与总营业额恒为 0，且不报错 | 改成带字段的 VO（`OrderSumVO`），由 MyBatis 按列别名走 setter 注入，行为稳定可测 |
| H2 无窗口函数 | 店铺榜的名次不能用 `row_number()` 在 SQL 里算 | 名次在 Java 侧按结果顺序补（`ManagerStatsServiceImpl.java:135-138`） |
| 中文查询参数被二次编码 | 自行 `URLEncoder.encode` 后再交给 `TestRestTemplate`，百分号会被再次编码（`%` → `%25`），服务端解不回来 | 直接传中文，由 `TestRestTemplate` 负责百分号编码 |

## 如何验收

```powershell
# 1. 后端：构建 + 跑全部契约测试（含 ManagerStatsApiTest，无需 MySQL / Docker）
powershell -ExecutionPolicy Bypass -File backend-java\tools\build.ps1

# 2. 起后端与数据库（MySQL 3307 / Redis 6379 / API 5000）
docker compose -p csu_db -f backend-java/docker-compose.yml up -d --build

# 3. 灌演示数据（注意上一节说的执行顺序）
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools/seed_demo_stats.sql

# 4. 前端
cd frontend\sjk
npm install
npm run check:theme   # 可选：校验设计 token 一致性（见「设计说明」）
npm run lint          # 可选：ESLint
npm run serve
```

访问路径与账号：打开 http://localhost:8080 → `/login` → 用 **`18682412818 / Aa.460734909`**（用户 CMX，role=1）登录 → 自动进入 `/manage` → 左侧「总览 → 数据看板」（默认页）。
用户端用 **`18873190398 / aA.460734909`**（用户 YCX，role=0）登录 → 自动进入 `/user`。
（登录按手机号匹配，填 CMX / YCX 会提示"用户名或密码错误"；登录页「演示账号」两行可一键填入。）
想看鉴权效果，开一个没登录的窗口直接请求统计接口，应返回 401 + `{"code": 1000, "msg": "用户未登录"}`：

```powershell
curl.exe -i http://127.0.0.1:5000/api/manager/stats/overview
```

**看板 8 个面板应看到什么**（检查清单）：

| # | 面板 | 检查点 |
| --- | --- | --- |
| 1 | KPI 指标卡 | 6 张卡都有数字（总订单数 358 左右、总营业额与店铺数 8、注册用户数 ≥2）；数字有千分位、总营业额超 1 万显示成 `x.x万`；未派发 / 派送中两张卡显示占总订单的百分比；切 7/30 天后第一、二张卡的"日均"提示跟着变 |
| 2 | 订单量与营业额走势 | 折线连续不断点（无单的日期是 0）；近 7 天曲线明显高于 30 天区间中段（演示数据刻意让最近 7 天升温）；页脚"区间订单 / 区间营业额 / 日均"与图上数据一致 |
| 3 | 订单状态构成 | 环形图三块齐全、中心显示订单总数；"已送达率"明显大于 0（若这里显示 0% 或全在"配送中"，就是演示数据执行顺序错了）；颜色与订单列表里的状态标签一致 |
| 4 | 订餐方式构成 | 人工订餐 / 网上订餐两块，图例带百分比，页脚有口径提示文字 |
| 5 | 配送员工作量对比 | 4 位配送员（摇摆羊、小亮、老八、赵三金）全部出现，没派过单的柱高为 0；tooltip 里能看到送达率；右上角在途 / 已完成合计与柱子相符 |
| 6 | 下单时段分布 | 24 根柱子；11–13 点与 17–19 点明显高于其他时段（双高峰）；右上 tag 显示高峰小时与单量 |
| 7 | 店铺业绩榜 | 8 家店按营业额降序、第 1–3 名颜色渐进；切「按订单量」顺序发生变化；Top 5 / Top 12 立即生效 |
| 8 | 店铺下钻抽屉 | 点任意一根柱子 → 右侧抽屉打开，标题是店铺名；5 项 KPI + 走势图 + 明细表（按日期倒序、含单均列）；抽屉内切 7/30 天只重取该店数据 |

另外两个交互可一并确认：顶栏"刷新"会重取总览与 6 张图并提示"数据已刷新"；从看板切到别的菜单再切回来不会重复请求（`visited` 机制），但会补一次 ECharts resize，图表不会塌成 0 尺寸。

> 顺带说明一处**呈现层**调整：管理端的三个订单列表（`/api/manager/unsend`、`sending`、`sended`）
> 在 SQL 里加了 `order by order_id desc`，让新单排在最前（`order_id` 是自增主键，天然就是下单先后）。
> 这里**没有**用 `create_time` 排序，因为 `sending_order` / `sended_order` 两个视图只 `select` 了
> `oorder` 的 10 个字段、**不含 `create_time`**（视图定义在 `init.sql` 里，属"零改动"范围），
> 三个列表用同一套排序口径也更一致。
> 响应结构与字段完全不变——`ManagerApiReadContractTest` 逐字节断言的用例在加排序后依然通过（各自数据集只有 1 行）。
> 这个改动是踩过坑的：一开始按 `create_time` 排序，H2 契约测试全绿、生产库却报 `Unknown column 'create_time'`，
> 原因是测试夹具的视图比生产多带了一列；测试库已按 `init.sql` 还原，并在 `testdb/schema.sql` 里留了注释说明
> "测试夹具不要比生产更全"。

## 前端可复用件说明

这一版把"每个页面各写一遍"的结构收敛成了下面 8 个复用件，看板是它们组合出来的第一个页面：

| 复用件 | 位置 | 解决什么问题 |
| --- | --- | --- |
| `AppShell` | `components/common/AppShell.vue` | 全站统一外壳（顶部条 + 侧栏 + 内容区）。原项目 `MyManage.vue` / `MyUser.vue` 各手写一份 `.header + .liner + .main`，样式已出现差异；收敛后两端共用一套视觉规范，并把"内容区固定高度、小屏溢出、大屏留白"换成 flex + 独立滚动。窄屏（< 1200px）自动收起侧栏 |
| `PagePanel` | `components/common/PagePanel.vue` | 列表页承载容器：标题 + 主色竖条 + 口径副标题 + 右侧操作槽 + 内容插槽，让「筛选 + 表格 + 分页」有统一范式 |
| `ChartCard` | `components/common/ChartCard.vue` | 图表卡片外壳：标题 / 口径副标题 / 右上工具槽 / 骨架屏 / 空态覆盖层 / 页脚槽。图表常驻 DOM，loading 与空态是绝对定位的覆盖层——ECharts 容器若被 `v-if` 反复销毁重建，resize / dispose 时序会变复杂 |
| `BaseChart` | `components/common/BaseChart.vue` | ECharts 容器：init / setOption / resize / dispose 四件事，外加防泄漏、自适应与点击事件归一化（只向上抛 `name` / `value` / `seriesName` / `dataIndex` / `data`，父组件不必感知 ECharts 的 `params` 结构） |
| `StatusTag` | `components/common/StatusTag.vue` | 订单 / 物流状态标签：0 待派单、1 配送中、2 已完成，颜色与看板图表同源；`labels` 可覆盖文案（物流页把 0/1 叫"在途 / 已结束"） |
| `listPager` | `mixins/listPager.js` | 列表页「筛选 + 分页」混入：接口仍全量返回 `tabledata`（契约不变），分页与关键字过滤放在前端；`pageSize: 0` 关闭分页；关键字变化自动回第一页；`clampPage()` 在删数据后校正越界页码 |
| `format.js` | `utils/format.js` | 模板层格式化：`fmtNum` / `fmtMoney` / `fmtCompact` / `fmtPercent` / `fmtText`，以及 `ORDER_STATUS` 状态字典；与图表侧格式化口径一致，避免同一个数字在两处显示不同 |
| `echarts.js` | `charts/echarts.js` | ECharts 5 按需注册（只进 Bar / Line / Pie 与必要组件）+ `campus-amber` 主题 + 色值常量 `CHART_COLORS` / `C` + `thousand` / `money` / `compact` / `percent` 四个格式化函数 |

其中 `BaseChart` 的防泄漏设计值得单独说明（这几条都是实际踩出来的）：

1. **实例放组件私有字段 `this._chart`，不进 `data`**：Vue 2 会递归劫持 `data` 上的对象，而 ECharts 实例结构庞大且带自引用，放进 `data` 只会产生无意义的响应式开销。
2. **全局 resize 订阅表**：`subscribers`（`Set`）+ 一个 `window` 监听服务所有图表实例；8 张图只挂 1 个监听器，而不是各自 `addEventListener`（拖动窗口时会重复触发），最后一个退订时移除监听。
3. **销毁时机**：`beforeDestroy` 中 `off('click')` + `dispose()` + 退订，并在 `setOption` / `resize` 前统一判 `isDisposed()`，避免"请求返回晚于组件销毁"这一典型泄漏与报错。
4. **`v-show` 隐藏时不 resize**：隐藏期间容器宽高为 0，此时 resize 会让画布彻底塌掉，因此 `resize()` 遇到 0 尺寸直接跳过；父组件切回看板时由 `visible` 属性驱动 `$nextTick` 补一次 resize（`MyManage.vue:144-150` 把 `statsVisible` 先置 `false` 再置回 `true`，就是为这个用途）。
5. **`setOption(option, false, true)`**：`notMerge = false`，数据刷新后保留用户此前的缩放 / 选中状态。
