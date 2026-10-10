# 管理端数据可视化 · 设计说明

> 本文档记录「校园快餐订餐管理系统」数据可视化改造的设计决策与实现约束。
> 面向读者：项目作者本人（面试讲解用）、以及后续接手维护的人。
> 代码实现见 `backend-java/src/main/java/com/csu/takeout/`（统计接口）与
> `frontend/sjk/src/components/ManageStats/`（看板前端）。

---

## 1. 为什么要做，以及和 sky-take-out 的区别

原项目（CSU 数据库课程设计）的管理端只有 8 个"查询 + 增删"页面，没有任何统计视图；
数据库里的视图、触发器这些"数据侧能力"在界面上完全看不见。本次改造做了两件事：

1. 新增 **管理端数据看板**（8 个面板），把视图与触发器维护的数据真正呈现出来；
2. 把整个前端从"早期手写布局"升级为**统一的后台系统骨架**，管理端与用户端共用一套外壳。

参考对象是作者的另一个项目 sky-take-out（Spring Boot 外卖系统），但**只借鉴优秀后台的共性**
（左导航 + 顶栏 + 内容区、列表页 = 筛选 + 表格 + 分页 + 弹窗表单、统计页 = KPI 卡 + 图表网格），
配色、信息架构、图表组合、接口形态全部按本项目主题重新设计。差异见第 4 节。

---

## 2. 技术约束（改造过程中始终遵守）

| 约束 | 具体做法 |
| --- | --- |
| 数据库零改动 | 表结构 / 视图 / 触发器 / `init.sql` 一行未改；统计 SQL 全部 `SELECT` |
| 既有接口零改动 | 19 个管理端 + 10 个用户端接口的路径、参数、响应体（含 Flask 兼容排版）完全不变 |
| 仅新增统计接口 | 新增 `/api/manager/stats/**`，响应沿用本项目的 `{"status":200,"data":{...}}` 形态 |
| 不升级 Vue | 仍是 Vue 2.6 + Element UI 2.15；ECharts 按需引入 5.x |
| 图表自封装 | 不用 vue-echarts 之类封装库（Vue 2 版本维护状态不确定），自己实现 `BaseChart` |
| 部署方式不变 | 统计接口并入现有 `backend-java` 镜像，`docker-compose.yml` 未改 |

---

## 3. 视觉设计：暖食琥珀 × 学院米白

### 3.1 主题取舍

主色需要在"食欲感"和"可信度"之间取平衡：

- 纯橙红（Element 默认 `#E6A23C` 一带）偏廉价，大面积使用会视觉疲劳；
- 纯学院蓝专业但丢掉了"吃"的语义，与校园快餐主题脱节。

最终选 **琥珀橙 `#E8622C` 作主色**（点缀、主行动、折线主色）+ **墨蓝 `#2A3B4C` 作深色侧栏**，
底色用 **米白 `#F6F4F0`** 而非冷灰白，卡片用 **暖白 `#FFFDF9`**——形成"暖色食欲 + 学院秩序"的组合。

### 3.2 颜色 token（唯一来源：`frontend/sjk/src/assets/styles/theme.css`）

| 角色 | 色值 | 用途 |
| --- | --- | --- |
| 主色 `--csu-primary` | `#E8622C` | 主按钮、选中菜单、KPI 主数、折线主色 |
| 主色深 / 浅 | `#C44E1E` / `#F58A5C` | hover / 强调 |
| 主色浅底 | `#FDEDE5` | 选中背景、tag 底 |
| 主色浅底文字 | `#B8431A` | 保证浅底上的对比度（WCAG AA） |
| 侧边栏 | `#2A3B4C` | 导航底（**深色**） |
| 侧栏 hover | `#35495D` | 菜单悬停 |
| 页面底 / 卡片 | `#F6F4F0` / `#FFFDF9` | 暖白而非冷灰 |
| 表头 | `#F0EBE4` | 表格表头暖灰 |
| 文字 主 / 次 / 弱 | `#26323D` / `#5C6B7A` / `#9AA7B4` | 三级文字层次 |
| 成功 / 警告 / 危险 / 信息 | `#2FA36B` / `#E0A020` / `#D6453D` / `#3E7CB1` | 语义色 |
| 图表序列色 | `#E8622C` `#2A3B4C` `#E0A020` `#2FA36B` `#3E7CB1` `#8C6BB1` `#D97A45` `#7A8B99` | 与 CSS 变量一一对应 |

### 3.3 与 sky-take-out 的差异点

| # | sky-take-out 的做法 | 本项目的做法 | 理由 |
| --- | --- | --- | --- |
| 1 | 亮蓝 `#409EFF` | 琥珀橙 `#E8622C` | 主题是校园快餐，暖色与"吃"的语义相关 |
| 2 | 浅色侧边栏 + 白色顶栏 | **深色**侧栏（墨蓝）+ 暖白顶栏 | 深色侧栏压住暖色，避免整屏发飘；也是与 sky 最直观的区分 |
| 3 | 四色渐变图标块 KPI 卡 | 左侧 4px 语义色条 + 等宽数字 + 底部口径说明 | 数字用等宽字体，刷新时不左右跳动 |
| 4 | 图表只有标题 | 标题 + **统计口径副标题**（如"oorder 按天聚合 · 服务端补零"） | 面试/演示时能直接讲清数据来源，避免"数字对不上"的质疑 |
| 5 | 状态用普通 tag | 状态标签与图表**同色**（待派单=琥珀、配送中=主色、已完成=绿） | 看板与列表共用一套颜色语义，不用二次记忆 |
| 6 | 统计接口返回逗号拼接字符串 | 全部返回结构化数组 + 服务端补零 | 前端可直接画图，折线不会因缺日断点 |

### 3.4 信息架构（管理端 4 组 9 页）

```
总览
└ 数据看板            ← 新增，默认落地页

点单
├ 待派单  (checked=0)  原「未发货订单」
├ 配送中  (checked=1)  原「已发货订单」
└ 已完成  (checked=2)

配送
├ 在途配送 (wuliu.ended=0)
└ 历史配送 (wuliu.ended=1)

门店
├ 店铺管理
├ 服务员管理
└ 配送员管理            原「送餐员管理」，与后端 dispatcher 统一称呼
```

**文案修正**：原界面的「未发货 / 已发货」描述的是数据库列 `checked`，而该列的真实语义是
**派发状态**（0 未派发 → 1 派送中 → 2 已送达），与"发货"无关。本次统一改为
「待派单 / 配送中 / 已完成」，并让看板、列表、状态标签三处用词与配色一致。

---

## 4. 图表 → 接口 → 实现 对照

| # | 面板 | 图表类型 | 接口 | 前端组件 | 关键交互 |
| --- | --- | --- | --- | --- | --- |
| ① | 经营总览 | 6 张 KPI 卡 | `GET /stats/overview` | `KpiCards.vue` | 提示文案随 7/30 天切换 |
| ② | 订单量与营业额走势 | 面积 + 折线（双 Y 轴） | `GET /stats/trend?days=` | `TrendChart.vue` | 近 7 / 30 天切换 |
| ③ | 订单状态构成 | 环形图 + 中心总数 | `GET /stats/status` | `StatusDonut.vue` | 底部显示已送达率 |
| ④ | 店铺业绩榜 | 横向条形图 | `GET /stats/shops?limit=&metric=` | `ShopRankChart.vue` | **点击柱子下钻**；按营业额/订单量切换；Top 5/8/12 |
| ⑤ | 订餐方式构成 | 饼图 | `GET /stats/orderway` | `OrderWayPie.vue` | 页脚说明"读 orderway 表，口径不同于实时聚合" |
| ⑥ | 配送员工作量对比 | 堆叠柱状图 | `GET /stats/dispatchers` | `DispatcherBar.vue` | 在途/已完成分段，tooltip 给送达率 |
| ⑦ | 下单时段分布 | 24 小时柱状图 | `GET /stats/hours` | `HourBar.vue` | 午市/晚市分色 + 峰值标线 |
| ⑧ | 店铺下钻明细 | 抽屉内柱+线 + 表格 | `GET /stats/shop/trend?shop_name=&days=` | `ShopDrillDrawer.vue` | 跟随页面 7/30 天选择 |

**口径提示（重要）**：`orderway.count` 与 `fastfood_shop.m_sale_v` 由**触发器增量维护**，
是独立于 `oorder` 的历史累计值；而看板其它面板是从 `oorder` 实时聚合的。
两者口径不同是数据库设计本身的特点，不是缺陷。界面上：
- 「订餐方式构成」页脚明确写出"直接读表，口径不同于实时聚合"；
- 「店铺管理」的"营业额估算（单价 × 销量）"页脚同样注明与看板营业额来源不同。

---

## 5. 只读保证与鉴权

- 统计 Service 标注 `@Transactional(readOnly = true)`，Mapper 全部是 `<select>`，
  **不触碰任何写路径**，因此不可能触发 `order_insert` / `order_insert_sale` /
  `order_update` / `order_delete` / `order_delete_sale` / `wuliu_insert` 的副作用。
- 「派送中 / 已送达」的状态判定交给数据库视图 `sending_order` / `sended_order`
  （视图内部已完成三表连接与 `checked` 过滤），Java 侧不写 JOIN、不做状态过滤——
  与项目既有约定一致。
- `/api/manager/stats/**` 纳入 `JwtTokenInterceptor` 的拦截路径；
  **既有 19 个管理端接口的鉴权行为一行未改**（Flask 原样不校验）。
  前端所有请求本来就会自动携带 `token` 头，因此对现有页面零影响；
  未携带/非法令牌访问统计接口返回 `401 + {"code":1000,"msg":"用户未登录"}`。

---

## 6. 前端可复用件

| 文件 | 解决的问题 |
| --- | --- |
| `components/common/AppShell.vue` | 顶栏（品牌 + 面包屑 + 刷新 + 用户菜单）+ 可折叠侧栏 + 内容区；管理端与用户端共用，替掉原先两份手写的 `.header/.liner/.main` |
| `components/common/PagePanel.vue` | 列表页统一标题条（标题 + 口径副标题 + 右侧操作槽），替掉原先每个页面各自的 64px 居中方块标题 |
| `components/common/ChartCard.vue` | 图表卡片外壳：标题 + 副标题 + 工具槽 + loading 骨架 + 空态兜底，保证 8 个面板交互一致 |
| `components/common/BaseChart.vue` | ECharts 生命周期封装（见下方防泄漏说明） |
| `components/common/StatusTag.vue` | 订单/物流状态标签，颜色与看板图表同源 |
| `charts/echarts.js` | ECharts 按需注册 + `campus-amber` 主题 + 千分位/金额/百分比格式化 |
| `utils/format.js` | 模板层格式化与业务字典（订单状态、订餐方式） |
| `mixins/listPager.js` | 前端「筛选 + 分页」；本项目列表接口是全量返回，接口不变的前提下收敛渲染行数 |

### BaseChart 的防泄漏设计

| 措施 | 原因 |
| --- | --- |
| ECharts 实例存在组件私有字段 `_chart`，**不放进 `data`** | Vue 2 会递归劫持 `data` 上的对象，ECharts 实例结构庞大且自引用，放进 data 会产生无意义的响应式开销 |
| window resize 用**全局订阅表**（`Set` + 单个监听器） | 一个页面 8 张图就是 8 个监听器，拖动窗口时重复触发 |
| `v-show` 隐藏（宽高为 0）时跳过 `resize()` | 否则画布会被 resize 成 0 尺寸，切回来一片空白 |
| `beforeDestroy` 中解除订阅 + `dispose()`，`setOption` 前判空 | 避免"请求返回晚于组件销毁"这类典型内存泄漏与报错 |
| 容器常驻 DOM，加载/空态用绝对定位覆盖层 | 避免 `v-if` 反复销毁重建 ECharts 容器导致的 resize/dispose 时序问题 |

---

## 7. 演示数据（含一个必须注意的触发器坑）

`tools/gen_seed_demo_stats.ps1` 生成 `tools/seed_demo_stats.sql`：把 355 条订单铺在**最近 30 天**
（近 7 天约 33 单/天、第 8–30 天约 5 单/天，保证 7 天 / 30 天切换都有观感），
并生成对应的 293 条 `wuliu` 记录。只插数据、不动结构。

**执行顺序不能改**：

```
1) 先 INSERT oorder   —— checked 一律写 0
2) 再 INSERT wuliu    —— 触发器 wuliu_insert 会把对应订单 checked 强制置为 1
3) 最后状态校正        —— 按 wuliu.ended 把 checked 修正为 2（已送达）或 1（在途）
```

原因：`wuliu_insert` 是 `AFTER INSERT ON wuliu` 的触发器，作用是"派发物流后把订单置为派送中"。
如果按直觉"先插订单（含已送达）、再插物流"，那 212 条「已送达」订单会被触发器全部改成「配送中」，
看板的状态分布会严重失真（这是本项目实际踩过的坑）。

---

## 8. 契约测试覆盖

`backend-java/src/test/java/com/csu/takeout/api/ManagerStatsApiTest.java`（14 个用例）：

- 7 个统计接口未携带 token 一律 401，且既有管理端接口仍不校验（零回归）；
- 日期序列补零（无订单的日期必须出现）、小时桶固定 24 个、峰值小时正确；
- 参数收敛（`days` 收敛到 `[1,90]`、`limit` 收敛到 `[1,20]`、非法 `metric` 回落 `money`）；
- 店铺不存在返回 `status=1000`；`left join` 保证未接单的配送员也出现在列表里（数量 0）；
- 只读性：连续调用两次结果完全一致，且不改变订单表数据。

测试隔离与 H2 兼容性上踩过的坑（已解决，记录备查）：

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 上下文重建后数据没复位 | H2 内存库名固定 + `DB_CLOSE_DELAY=-1`，重跑 `data.sql` 必然撞主键 | `application-h2test.yml` 的库名加 `${random.uuid}` |
| `date_format()` 报 SQL 错误 | H2 1.4.200 无该函数 | 改用 `substring(create_time, 1, 10)` |
| `hour()` 报 "Column create_time must be in the GROUP BY list" | H2 把 `hour()` 当聚合函数 | `group by` 里重复整个表达式而非别名 |
| 聚合结果取不到值 | 用 `Map<String,Object>` 接聚合列在 H2 下未映射 | 改为带字段的 VO（`OrderSumVO` / `StatsDailyRowVO`） |
| 统计用例偶发看到 4 条/5 条订单 | 同层写操作测试类会新增订单，Spring 上下文按配置共享 | 本类标 `@DirtiesContext(AFTER_CLASS)` |

---

## 9. 验收清单

```powershell
# 1) 后端构建 + 全部契约测试（70 个用例）
powershell -ExecutionPolicy Bypass -File backend-java\tools\build.ps1

# 2) 数据库（MySQL 8.0 容器，端口 3307）
docker compose -p csu_db -f backend-java\docker-compose.yml up -d mysql redis
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools\seed_test_accounts.sql
docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools\seed_demo_stats.sql

# 3) 先后端再前端
java -jar backend-java\target\csu-takeout.jar
cd frontend\sjk; npm run serve      # http://localhost:8080
```

测试账号：管理端 `CMX / Aa.460734909`、用户端 `YCX / aA.460734909`。

管理端登录后应看到（演示数据下）：KPI 总订单 358 / 营业额 5,067 元 / 未派发 64 / 派送中 81 /
店铺 8 / 用户 2；走势图 7 天与 30 天两档都连续无断点；状态环形图三段颜色与列表状态标签一致；
店铺榜 8 行并可点击下钻；配送员 4 根堆叠柱；时段分布呈午市 + 晚市双峰。

---

## 10. 个人信息编辑（含一处必须知道的取舍）

个人中心新增「修改资料」能力，接口是本项目**新增**的三个（既有接口一行未改）：

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/api/user/usermsg` | 查询（既有，Flask 契约） |
| PUT | `/api/user/usermsg/edit` | 改 用户名 / 真实姓名 / 性别 / 年龄 / 邮箱 |
| PUT | `/api/user/usermsg/pwd` | 改密码（复用 `UserService.changePassword`，语义与既有 `POST /api/user/pwd_chg` 完全一致） |

### 为什么不复用 POST `/api/user/usermsg`

Flask 版该路径的装饰器写了 `methods=["POST", "GET"]`，但函数体里**只有 GET 分支**，
POST 会落到隐式 `return None` → 500。契约中的"既有接口"只包含已实现的行为，
所以不去"补全"一个原本会报错的 POST，而是新增语义明确的路径。

### 联系电话为什么必须只读

`user_msg.phone` 是查询接口的**定位键**：

```sql
-- Flask app.py:236 / Java UserMsgMapper.xml，两边一致
select * from user_msg where phone = ?
```

一旦允许用户修改它，改完就再也查不到自己的记录（接口会返回没有 `data` 键的
`{"status":200}`）。`user_msg` 与 `user` 之间其实只有 `user_msg.id → user.id` 这条外键，
`phone` 只是**恰好**与 `user.telephone` 相等才没暴露问题。

因此本页把「联系电话」做成只读项，并在界面上直接写明原因；请求 DTO
（`UserMsgUpdateDTO`）也只声明 5 个可改字段——配合 Jackson 的
`FAIL_ON_UNKNOWN_PROPERTIES`（严格模式），多传任何未声明字段会直接被拒绝并返回
`{"status":1000,"msg":"未知错误"}`，不会出现"传了但被静默忽略"的错觉。

若将来确实要支持改手机号，正确做法是把查询改成走外键
（`join user u on u.id = um.id where u.telephone = ?`）——物理表与视图零改动，
但属于既有接口的语义变更，需单独确认。

---

## 11. 遗留与后续可做

- 列表页分页为**前端分页**（接口是全量返回且不能改）；若将来允许改接口，可换成服务端分页。
- `wuliu.ended` 没有可写的接口（沿用原状），因此"历史配送"页的数据来自演示数据脚本，
  页面上已按此说明标注。
- 看板未做自动轮询刷新，靠顶栏刷新按钮手动触发（避免演示时数字乱跳）。
- 左侧导航的图标与文字改用 flex 垂直居中（原来依赖行高继承，不同图标的字形墨迹高度
  不一致，会出现个别图标偏上/偏下）。注意 Element 会给 `el-menu-item` 写行内
  `padding-left: 20px`，覆盖它必须用 `!important`；给 `slot="title"` 的 span 也要显式
  清掉 inline padding，因此菜单标题统一加了 `.side__label` class 作为定位锚点——
  不用 `span[slot='title']` 属性选择器，因为 Element 重建子节点时不保证该属性留在最终 DOM 上。
