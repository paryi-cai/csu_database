/**
 * ECharts 5 统一封装。
 *
 * 为什么不用 vue-echarts 之类的封装库：
 *   本项目是 Vue 2.6 + webpack 5（@vue/cli 5）的旧依赖树，主流 ECharts 封装库的
 *   新版本都以 Vue 3 为主，Vue 2 版本的维护状态与 peer 依赖都不确定。
 *   图表生命周期只有 init / setOption / resize / dispose 四件事，自己封装 60 行
 *   足够可控，也便于处理"防泄漏"和"响应式"这两个真正的坑。
 *
 * 按需注册：只 import 用到的图表与组件，避免整包体积（本项目用不到地图/3D/GL）。
 *
 * 注意：主题色在这里<b>另存一份</b>（图表读不到 CSS 变量），
 *       与 src/assets/styles/theme.css 中的 --csu-c* 必须保持一致。
 */
import * as echarts from 'echarts/core'

import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
    GridComponent,
    TooltipComponent,
    LegendComponent,
    TitleComponent,
    DatasetComponent,
    MarkLineComponent,
    ToolboxComponent,
    AxisPointerComponent
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
    BarChart,
    LineChart,
    PieChart,
    GridComponent,
    TooltipComponent,
    LegendComponent,
    TitleComponent,
    DatasetComponent,
    MarkLineComponent,
    ToolboxComponent,
    AxisPointerComponent,
    LabelLayout,
    UniversalTransition,
    CanvasRenderer
])

/**
 * 页面/图表共用的色值常量。
 *
 * 定位：**这是配色的事实来源**——CANVAS 渲染的 ECharts 读不到 CSS 变量，必须传具体色值；
 * 为了让"DOM 里的主色"与"图表里的主色"不漂移，凡是要与主题一致的色值都从这里取，
 * 而不是在组件模板里写 `accent="#E8622C"` 这类字面量。
 * theme.css 里的同名变量与这里的对应关系由 `npm run check:theme` 断言。
 *
 * ⚠️ 声明顺序有约束：`CHART_COLORS` 引用了 `C`，所以 `C` 必须写在前面。
 * `const` 不具备跨声明提升（TDZ），顺序颠倒会在模块加载时直接抛
 * "Cannot access 'C' before initialization"——而 app.js 第一件事就是 import 本模块，
 * 结果是整站白屏（本项目实际踩过一次）。
 */
export const C = {
    /** 主色（= --csu-primary） */
    primary: '#E8622C',
    primaryLight: '#F58A5C',
    primaryBg: '#FDEDE5',
    /** 主色浅底上的文字色（= --csu-primary-text），保证对比度 */
    primaryText: '#B8431A',
    /** 深色结构色：侧栏底、图表里的"次主角"（= --csu-sidebar） */
    ink: '#2A3B4C',
    amber: '#E0A020',
    green: '#2FA36B',
    /** 成功色的高亮态（hover / emphasis） */
    greenLight: '#3FC184',
    /** 成功色浅底上的文字色（= 与 --csu-success 同族的深色） */
    greenText: '#1F7A4D',
    blue: '#3E7CB1',
    purple: '#8C6BB1',
    danger: '#D6453D',
    /** 图表序列里的中性灰（= --csu-c8） */
    grey: '#7A8B99',
    text: '#26323D',
    textSub: '#5C6B7A',
    textWeak: '#9AA7B4',
    border: '#E8E2D9',
    borderLight: '#F1ECE5',
    panel: '#FFFDF9',
    headerBg: '#F0EBE4'
}

/** 图表序列色（顺序即取色顺序，与 C 里的主色族保持一致；必须定义在 C 之后） */
export const CHART_COLORS = [
    C.primary, C.ink, C.amber, C.green,
    C.blue, C.purple, '#D97A45', C.grey
]

const FONT_FAMILY = '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", ' +
    '"Hiragino Sans GB", "Microsoft YaHei", Helvetica, Arial, sans-serif'

/** 直角坐标系的公共样式：网格线用暖色浅灰，去掉竖线，坐标轴弱化 */
const axisDefaults = {
    axisLine: {
        show: true,
        lineStyle: { color: C.border }
    },
    axisTick: { show: false },
    axisLabel: {
        color: C.textSub,
        fontSize: 12,
        fontFamily: FONT_FAMILY
    },
    splitLine: {
        show: true,
        lineStyle: { color: C.borderLight, type: 'solid' }
    },
    splitArea: { show: false }
}

/**
 * 图表主题：campus-amber
 * 只覆盖配色、字体、tooltip / legend 外观，坐标轴细节放在各 option 里按图定制
 * （折线图不要竖网格线，条形图不要横网格线，差异较大）。
 */
export const THEME_NAME = 'campus-amber'

export const campusAmberTheme = {
    color: CHART_COLORS,
    backgroundColor: 'transparent',
    textStyle: {
        fontFamily: FONT_FAMILY,
        color: C.textSub
    },
    title: {
        textStyle: { color: C.text, fontSize: 14, fontWeight: 600 },
        subtextStyle: { color: C.textWeak, fontSize: 12 }
    },
    legend: {
        textStyle: { color: C.textSub, fontSize: 12 },
        icon: 'roundRect',
        itemWidth: 10,
        itemHeight: 10,
        itemGap: 14
    },
    tooltip: {
        backgroundColor: 'rgba(42, 59, 76, .92)',
        borderWidth: 0,
        padding: [8, 12],
        textStyle: { color: '#FFFFFF', fontSize: 12, fontFamily: FONT_FAMILY },
        extraCssText: 'border-radius:6px;box-shadow:0 4px 14px rgba(42,59,76,.18);'
    },
    categoryAxis: axisDefaults,
    valueAxis: axisDefaults,
    line: {
        symbolSize: 6,
        smooth: true,
        lineStyle: { width: 2.4 }
    },
    bar: {
        itemStyle: { borderRadius: [4, 4, 0, 0] }
    },
    pie: {
        itemStyle: { borderColor: C.panel, borderWidth: 2 },
        label: { color: C.textSub },
        labelLine: { lineStyle: { color: C.border } }
    }
}

echarts.registerTheme(THEME_NAME, campusAmberTheme)

/* ============================================================================
 * 格式化工具：全站数字口径统一（千分位 / 金额 / 百分比）
 * ========================================================================== */

/** 千分位整数或小数：1234567 -> 1,234,567 */
export function thousand(value, digits = 0) {
    const num = Number(value)
    if (!isFinite(num)) return '0'
    return num.toLocaleString('zh-CN', {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
    })
}

/** 金额：默认显示整数元（本项目 order_money 是 int，没有小数） */
export function money(value, digits = 0) {
    return thousand(value, digits)
}

/** 大数字紧凑显示：12345 -> 1.2万（用于 KPI 卡，避免撑破卡片） */
export function compact(value) {
    const num = Number(value)
    if (!isFinite(num)) return '0'
    if (Math.abs(num) >= 10000) return (num / 10000).toFixed(1).replace(/\.0$/, '') + '万'
    return thousand(num)
}

/** 百分比：0.6154 -> 61.5% */
export function percent(value, digits = 1) {
    const num = Number(value)
    if (!isFinite(num)) return '0%'
    return num.toFixed(digits).replace(/\.0$/, '') + '%'
}

export default echarts
