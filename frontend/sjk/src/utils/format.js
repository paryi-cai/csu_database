/**
 * 展示层格式化工具。
 *
 * 与 src/charts/echarts.js 里的格式化函数职责区分：
 *   - 那边服务于 ECharts 的 axisLabel / tooltip / 图表内文案；
 *   - 这里服务于模板（表格单元格、KPI、标签），是纯字符串工具，不依赖 ECharts。
 * 两者的数值口径保持一致（千分位、金额取整），避免同一个数字在图表和表格里显示不一样。
 */

/** 千分位：12345 -> 12,345 */
export function fmtNum(value, digits = 0) {
    const num = Number(value)
    if (!isFinite(num)) return value === null || value === undefined ? '-' : String(value)
    return num.toLocaleString('zh-CN', {
        minimumFractionDigits: digits,
        maximumFractionDigits: digits
    })
}

/** 金额：本项目 order_money / price 都是整数元，默认不带小数 */
export function fmtMoney(value, digits = 0) {
    return fmtNum(value, digits)
}

/** 空值兜底显示，避免表格里出现 "undefined" 或空白 */
export function fmtText(value, placeholder = '—') {
    if (value === null || value === undefined || value === '') return placeholder
    return String(value)
}

/* ==================== 业务字典 ==================== */

/**
 * 订单状态（oorder.checked）——**这是状态文案与语义色的唯一事实来源**。
 *
 * 与数据库语义严格一致：0 未派发 → 1 派送中 → 2 已送达。
 * 展示文案统一用「待派单 / 配送中 / 已完成」，与看板、菜单保持一致
 * （原界面用"未发货/已发货"描述派发状态，与 checked 的含义不符）。
 *
 * `type` 供 el-tag 使用；`tone` 是状态标签自己的浅底/描边配色，数值与
 * `charts/echarts.js` 的 `C.amber / C.primary / C.green` 一致——放在这里而不是
 * 从 echarts.js 取，是为了让 StatusTag 不因为一个色值而把整个 ECharts 运行时拉进依赖图。
 */
export const ORDER_STATUS = {
    0: {
        label: '待派单',
        type: 'warning',
        tone: { color: '#B8431A', bg: 'rgba(224,160,32,.14)', dot: '#E0A020' }
    },
    1: {
        label: '配送中',
        type: 'primary',
        tone: { color: '#B8431A', bg: 'rgba(232,98,44,.13)', dot: '#E8622C' }
    },
    2: {
        label: '已完成',
        type: 'success',
        tone: { color: '#1F7A4D', bg: 'rgba(47,163,107,.13)', dot: '#2FA36B' }
    }
}

/** 状态未知时的兜底配色（灰） */
export const ORDER_STATUS_FALLBACK_TONE = {
    color: '#5C6B7A',
    bg: 'rgba(122,139,153,.14)',
    dot: '#7A8B99'
}

/** 订餐方式候选（与 orderway 表既有两条记录一致） */
export const ORDER_WAYS = ['人工订餐', '网上订餐']

/** 取状态文案；未知状态回退成状态码本身 */
export function orderStatusText(checked) {
    const item = ORDER_STATUS[Number(checked)]
    return item ? item.label : String(checked)
}
