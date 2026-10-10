# frontend/sjk — Vue 2 前端

校园快餐订餐管理系统前端：用户端（逛店铺/下单/订单跟踪/个人信息）+ 管理端
（数据看板/订单派发/物流/门店管理），通过 Axios 直连 `127.0.0.1:5000` 后端。

## 技术栈

Vue 2.6 · Vue Router 3 · Element UI 2.15 · Axios · ECharts 5 · @vue/cli 5（webpack 5）

## 目录

- `src/api/index.js` — Axios 封装（自动携带 `token`）+ `statsApi`（看板 8 个接口，独立超时与解包）
- `src/router/index.js` — 路由 `/login` `/user` `/manage`；`meta.title` 驱动浏览器标签页标题
- `src/charts/echarts.js` — ECharts 按需注册 + `campus-amber` 主题 + 数字格式化
- `src/components/common/` — AppShell / PagePanel / ChartCard / BaseChart / StatusTag
- `src/components/ManageStats/` — 数据看板：KPI、双轴趋势、状态环、店铺榜+下钻等 8 个面板
- `src/mixins/listPager.js` — 列表页「关键字筛选 + 前端分页」（接口保持全量返回）
- `src/utils/format.js` — 展示格式化 + `ORDER_STATUS` 状态文案/配色（唯一事实来源）
- `scripts/check-theme-tokens.js` — 校验 CSS 变量定义与引用一致

## 运行

```bash
npm install
npm run serve     # http://localhost:8080
npm run build     # 生产构建
npm run lint      # ESLint 检查
```
