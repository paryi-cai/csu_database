import axios from "axios"
// import { Store } from "vuex";

const aaxios = axios.create({
    // baseURL: "http://192.168.192.1:5000",
    baseURL: "http://127.0.0.1:5000",
    timeout: 5000
})

//在发请求之前：请求拦截器可以检测到，加入token

aaxios.interceptors.request.use((config) => {
    let access_token = localStorage.getItem('token');
    if (access_token) {
        config.headers.token = localStorage.getItem('token')
    }

    return config;
});

/**
 * 统计接口专用超时（20s）。
 *
 * 复用同一个 axios 实例（同一套 baseURL 与 token 拦截器），只把超时放宽：
 * 原实例的 5s 对"逐页查询"够用，但统计接口是聚合查询——数据量上去之后
 * 5 秒可能不够，而统计页一慢就整页报"网络故障"，观感很差。
 * 因此这里按请求单独覆盖 timeout，不影响任何既有接口的行为。
 */
const STATS_TIMEOUT = 20000;

/**
 * 统一解包统计接口响应。
 *
 * 契约：{"status": 200, "data": {...}}，失败为 {"status": 1000, "msg": "..."}。
 * 这里把业务失败转成 reject，页面只需关心 data 与 catch，
 * 不用在每个图表里重复判断 status。
 */
function unwrapStats(url, params) {
    return aaxios
        .get(url, { params: params, timeout: STATS_TIMEOUT })
        .then((res) => {
            const body = res.data || {};
            if (body.status === 200 && body.data !== null && body.data !== undefined) {
                return body.data;
            }
            const err = new Error(body.msg || '统计数据获取失败');
            err.bizStatus = body.status;
            throw err;
        });
}

/**
 * 从统计接口返回的 data 中取出列表。
 *
 * 本组接口的 data 有两种形态，前端不做特判就很容易取空：
 *   - 纯数组：/stats/dispatchers
 *   - 对象包一层 items：/stats/status、/stats/shops、/stats/orderway、/stats/hours ...
 * 统一走这个函数，组件里就不必各自猜结构（曾因此让配送员图表拿到空数组）。
 *
 * @param {*} data     接口返回的 data
 * @param {*} fallback 可选：当 data 是对象时，从该值取 items
 * @returns {Array}
 */
export function toList(data, fallback) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(fallback)) return fallback;
    if (data && Array.isArray(data.items)) return data.items;
    return [];
}

/* ==================== 管理端 · 数据统计（只读） ==================== */
export const statsApi = {
    /** 经营总览：总订单数 / 总营业额 / 店铺数 / 注册用户数 / 未派发 / 派送中 / 已送达 */
    overview: () => unwrapStats('/api/manager/stats/overview'),

    /** 订单量与营业额走势：days = 7 | 30 */
    trend: (days) => unwrapStats('/api/manager/stats/trend', { days: days }),

    /** 订单状态分布（checked 0/1/2） */
    status: () => unwrapStats('/api/manager/stats/status'),

    /** 店铺业绩榜：limit 条，metric = money | count */
    shops: (limit, metric) => unwrapStats('/api/manager/stats/shops', { limit: limit, metric: metric }),

    /** 单店铺走势（Top 榜下钻） */
    shopTrend: (shopName, days) =>
        unwrapStats('/api/manager/stats/shop/trend', { shop_name: shopName, days: days }),

    /** 订餐方式构成（读 orderway 表，由触发器维护） */
    orderway: () => unwrapStats('/api/manager/stats/orderway'),

    /** 配送员工作量对比（wuliu 按 disp_id 聚合）；注意本接口 data 直接是数组 */
    dispatchers: () => unwrapStats('/api/manager/stats/dispatchers'),

    /** 下单时段分布（24 小时） */
    hours: () => unwrapStats('/api/manager/stats/hours')
};

//对外暴露
export default aaxios;