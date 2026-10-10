<template>
    <div class="csu-page">
        <!-- ==================== KPI 区 ==================== -->
        <div class="kpi-grid">
            <kpi-card v-for="card in kpiCards" :key="card.label" v-bind="card"></kpi-card>
        </div>

        <!-- ==================== 主趋势区（双 Y 轴） ==================== -->
        <trend-chart ref="trend" :visible="visible" :initial-days="activeDays" @days-change="onDaysChange">
        </trend-chart>

        <!-- ==================== 结构分析：卖什么 ==================== -->
        <div class="csu-grid csu-grid--2">
            <status-donut ref="status" :visible="visible"></status-donut>
            <order-way-pie ref="orderway" :visible="visible"></order-way-pie>
        </div>

        <!-- ==================== 结构分析：谁在送 / 什么时候下单 ==================== -->
        <div class="csu-grid csu-grid--2">
            <dispatcher-bar ref="dispatcher" :visible="visible"></dispatcher-bar>
            <hour-bar ref="hour" :visible="visible"></hour-bar>
        </div>

        <!-- ==================== 店铺榜 + 下钻 ==================== -->
        <shop-rank-chart ref="shopRank" :visible="visible" @drill="onDrill"></shop-rank-chart>

        <shop-drill-drawer :visible.sync="drillVisible" :shop="drillShop" :initial-days="activeDays">
        </shop-drill-drawer>
    </div>
</template>

<script>
import KpiCard from '@/components/ManageStats/KpiCards.vue'
import TrendChart from '@/components/ManageStats/TrendChart.vue'
import StatusDonut from '@/components/ManageStats/StatusDonut.vue'
import ShopRankChart from '@/components/ManageStats/ShopRankChart.vue'
import OrderWayPie from '@/components/ManageStats/OrderWayPie.vue'
import DispatcherBar from '@/components/ManageStats/DispatcherBar.vue'
import HourBar from '@/components/ManageStats/HourBar.vue'
import ShopDrillDrawer from '@/components/ManageStats/ShopDrillDrawer.vue'
import { statsApi } from '@/api/index'
import { thousand, percent } from '@/charts/echarts'

/**
 * 管理端数据看板。
 *
 * 职责划分：本页只负责「KPI 区 + 图表编排 + 下钻联动」，
 * 每张图自己管理请求与空态（各图口径不同，集中在一处反而难维护）。
 *
 * KPI 区与趋势图共用 /stats/overview 与 /stats/trend 的结果做"环比日均"提示，
 * 因此这里只发一次 overview 请求，避免每张卡各发一次。
 */
export default {
    name: 'StatsDashboard',
    components: {
        KpiCard,
        TrendChart,
        StatusDonut,
        ShopRankChart,
        OrderWayPie,
        DispatcherBar,
        HourBar,
        ShopDrillDrawer
    },
    props: {
        visible: { type: Boolean, default: true }
    },
    data() {
        return {
            loading: true,
            overview: {
                total_orders: 0,
                total_revenue: 0,
                shop_count: 0,
                user_count: 0,
                unsend_count: 0,
                sending_count: 0,
                sended_count: 0
            },
            /** 与趋势图右上角联动：KPI 的"日均"提示需要知道当前看的是 7 天还是 30 天 */
            activeDays: 7,
            drillVisible: false,
            drillShop: {}
        }
    },
    computed: {
        kpiCards() {
            const o = this.overview || {}
            const total = Number(o.total_orders || 0)
            const days = this.activeDays
            const orderAvg = days ? total / days : 0
            const revAvg = days ? Number(o.total_revenue || 0) / days : 0

            return [
                {
                    label: '总订单数',
                    value: total,
                    unit: '单',
                    icon: 'el-icon-s-order',
                    accent: '#E8622C',
                    hint: this.loading ? '加载中…' : '近 ' + days + ' 天日均 ' + orderAvg.toFixed(1) + ' 单'
                },
                {
                    label: '总营业额',
                    value: o.total_revenue || 0,
                    unit: '元',
                    icon: 'el-icon-coin',
                    accent: '#2FA36B',
                    compactMode: true,
                    hint: this.loading ? '加载中…' : '近 ' + days + ' 天日均 ' + thousand(revAvg) + ' 元'
                },
                {
                    label: '未派发订单',
                    value: o.unsend_count || 0,
                    unit: '单',
                    icon: 'el-icon-warning-outline',
                    accent: '#D6453D',
                    hint: total ? '占总订单 ' + percent((Number(o.unsend_count || 0) / total) * 100) : '暂无订单'
                },
                {
                    label: '派送中订单',
                    value: o.sending_count || 0,
                    unit: '单',
                    icon: 'el-icon-bicycle',
                    accent: '#E0A020',
                    hint: total ? '占总订单 ' + percent((Number(o.sending_count || 0) / total) * 100) : '暂无订单'
                },
                {
                    label: '店铺数',
                    value: o.shop_count || 0,
                    unit: '家',
                    icon: 'el-icon-s-shop',
                    accent: '#3E7CB1',
                    hint: '已入驻门店'
                },
                {
                    label: '注册用户数',
                    value: o.user_count || 0,
                    unit: '位',
                    icon: 'el-icon-user',
                    accent: '#8C6BB1',
                    hint: '含管理端账号'
                }
            ]
        }
    },
    created() {
        this.loadOverview()
    },
    methods: {
        loadOverview() {
            this.loading = true
            statsApi
                .overview()
                .then((data) => {
                    this.overview = data || this.overview
                })
                .catch(() => {
                    this.$message({ message: '总览指标加载失败，请确认后端统计接口可用', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        },
        onDrill(row) {
            this.drillShop = row
            this.drillVisible = true
        },
        /**
         * 接收趋势图的区间切换（7 / 30 天）。
         *
         * 作用范围：KPI 卡的"近 N 天日均"文案 + 下钻抽屉的默认区间。
         * 没有这个联动时，activeDays 恒为 7，切到 30 天后 KPI 文案会与图表口径不符。
         */
        onDaysChange(days) {
            this.activeDays = days
        },
        /**
         * 顶栏"刷新"按钮的转发入口：重取总览 + 6 张图。
         *
         * 用显式 ref 而不是遍历 this.$children 按组件名匹配：
         * $children 只保证是"直接子组件"且顺序不保证、也不包含更深层级，
         * 一旦以后把图表包进一层容器（例如再加一个 grid 包装组件），
         * 按名字查找会静默失效（刷新按钮看起来没反应）。ref 是显式契约，改名时会立刻报错。
         */
        refresh() {
            this.loadOverview()
            const refs = ['trend', 'status', 'shopRank', 'orderway', 'dispatcher', 'hour']
            refs.forEach((ref) => {
                const child = this.$refs[ref]
                if (child && typeof child.load === 'function') child.load()
            })
            this.$message({ message: '数据已刷新', type: 'success' })
        }
    }
}
</script>

<style scoped>
/* 6 张 KPI 卡：宽屏 3 列，超过 1600px 时 6 列一行放下 */
.kpi-grid {
    display: grid;
    gap: var(--csu-page-pad);
    grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (min-width: 1680px) {
    .kpi-grid {
        grid-template-columns: repeat(6, minmax(0, 1fr));
    }
}

@media (max-width: 1000px) {
    .kpi-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 640px) {
    .kpi-grid {
        grid-template-columns: minmax(0, 1fr);
    }
}
</style>
