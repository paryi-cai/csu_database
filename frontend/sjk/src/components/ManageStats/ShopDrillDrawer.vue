<template>
    <el-drawer :title="null" :visible.sync="innerVisible" direction="rtl" size="620px" :wrapper-closable="true"
        custom-class="drill-drawer" @close="onClose">
        <div slot="title" class="drill-hd">
            <span class="drill-hd__bar"></span>
            <span class="drill-hd__name">{{ shop.shop_name || '店铺明细' }}</span>
            <span class="drill-hd__tag">店铺业绩下钻</span>
        </div>

        <div v-loading="loading" class="drill">
            <div class="drill__kpis">
                <div class="drill-kpi">
                    <em>区间营业额</em>
                    <b class="csu-num">{{ money(data.revenue_total) }}</b>
                    <span>元</span>
                </div>
                <div class="drill-kpi">
                    <em>区间订单量</em>
                    <b class="csu-num">{{ num(data.order_total) }}</b>
                    <span>单</span>
                </div>
                <div class="drill-kpi">
                    <em>日均单量</em>
                    <b class="csu-num">{{ avg }}</b>
                    <span>单/天</span>
                </div>
                <div class="drill-kpi">
                    <em>产品单价</em>
                    <b class="csu-num">{{ money(shop.price) }}</b>
                    <span>元</span>
                </div>
                <div class="drill-kpi">
                    <em>数据库销量 m_sale_v</em>
                    <b class="csu-num">{{ num(shop.sale) }}</b>
                    <span>单</span>
                </div>
            </div>

            <div class="drill__toolbar">
                <span class="drill__section">近 {{ days }} 天走势</span>
                <el-radio-group v-model="days" size="mini" @change="load">
                    <el-radio-button :label="7">7 天</el-radio-button>
                    <el-radio-button :label="30">30 天</el-radio-button>
                </el-radio-group>
            </div>

            <div v-if="!loading && isEmpty" class="drill__empty">
                <i class="el-icon-data-analysis"></i>
                <p>该店在所选区间内没有订单</p>
            </div>
            <base-chart v-else :option="option" height="240px" :visible="innerVisible" :loading="loading"></base-chart>

            <div v-if="!isEmpty" class="drill__table">
                <el-table :data="tableRows" size="mini" class="csu-table" max-height="240" stripe>
                    <el-table-column prop="date" label="日期" width="120" align="center"></el-table-column>
                    <el-table-column prop="orders" label="订单量（单）" align="right">
                        <template slot-scope="scope">
                            <span class="csu-num">{{ num(scope.row.orders) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="revenue" label="营业额（元）" align="right">
                        <template slot-scope="scope">
                            <span class="csu-num">{{ money(scope.row.revenue) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="单均（元）" align="right">
                        <template slot-scope="scope">
                            <span class="csu-num">{{ scope.row.orders ? (scope.row.revenue / scope.row.orders).toFixed(1) : '0.0' }}</span>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </div>
    </el-drawer>
</template>

<script>
import BaseChart from '@/components/common/BaseChart.vue'
import { statsApi } from '@/api/index'
import { C, thousand } from '@/charts/echarts'

/**
 * 店铺下钻抽屉：Top 榜点柱子后从这里展开。
 *
 * 只请求一个接口（/stats/shop/trend），区间默认跟随页面当前选择，
 * 用于回答"这家店为什么排在前面"——是近一周冲上来的，还是长期稳定。
 */
export default {
    name: 'ShopDrillDrawer',
    components: { BaseChart },
    props: {
        visible: { type: Boolean, default: false },
        shop: { type: Object, default: () => ({}) },
        initialDays: { type: Number, default: 7 }
    },
    data() {
        return {
            innerVisible: this.visible,
            days: this.initialDays,
            loading: false,
            data: { dates: [], order_counts: [], revenues: [], order_total: 0, revenue_total: 0 }
        }
    },
    computed: {
        isEmpty() {
            return !this.data.dates || this.data.dates.length === 0
        },
        avg() {
            if (!this.data.dates || !this.data.dates.length) return '0.0'
            return (Number(this.data.order_total || 0) / this.data.dates.length).toFixed(1)
        },
        /** 表格按日期倒序，最近一天在最上面 */
        tableRows() {
            const dates = this.data.dates || []
            const counts = this.data.order_counts || []
            const revenues = this.data.revenues || []
            return dates
                .map((d, i) => ({ date: d, orders: Number(counts[i] || 0), revenue: Number(revenues[i] || 0) }))
                .reverse()
        },
        option() {
            const dates = this.data.dates || []
            const counts = this.data.order_counts || []
            const revenues = this.data.revenues || []

            return {
                grid: { top: 30, left: 8, right: 8, bottom: 4, containLabel: true },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'line', lineStyle: { color: C.primaryLight, type: 'dashed' } },
                    formatter: (params) => {
                        if (!params || !params.length) return ''
                        return '<div style="font-weight:600;margin-bottom:4px">' + params[0].axisValue + '</div>' +
                            params.map((p) => '<div style="display:flex;justify-content:space-between;gap:18px">' +
                                '<span>' + p.marker + p.seriesName + '</span><b>' + thousand(p.value) +
                                (p.seriesName === '营业额' ? ' 元' : ' 单') + '</b></div>').join('')
                    }
                },
                legend: {
                    top: 0,
                    right: 0,
                    itemWidth: 9,
                    itemHeight: 9,
                    textStyle: { color: C.textSub, fontSize: 12 }
                },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    data: dates,
                    axisLabel: {
                        color: C.textSub,
                        fontSize: 11,
                        interval: dates.length > 10 ? Math.ceil(dates.length / 7) : 0
                    },
                    axisLine: { lineStyle: { color: C.border } },
                    axisTick: { show: false }
                },
                yAxis: [
                    {
                        type: 'value',
                        minInterval: 1,
                        axisLine: { show: false },
                        axisTick: { show: false },
                        axisLabel: { color: C.textSub, fontSize: 11 },
                        splitLine: { lineStyle: { color: C.borderLight } }
                    },
                    {
                        type: 'value',
                        axisLine: { show: false },
                        axisTick: { show: false },
                        axisLabel: { color: C.textSub, fontSize: 11 },
                        splitLine: { show: false }
                    }
                ],
                series: [
                    {
                        name: '订单量',
                        type: 'bar',
                        barWidth: '42%',
                        data: counts,
                        itemStyle: { color: 'rgba(232,98,44,.78)', borderRadius: [3, 3, 0, 0] },
                        emphasis: { itemStyle: { color: C.primary } }
                    },
                    {
                        name: '营业额',
                        type: 'line',
                        yAxisIndex: 1,
                        data: revenues,
                        smooth: true,
                        symbol: 'circle',
                        symbolSize: 6,
                        lineStyle: { width: 2.2, color: C.ink },
                        itemStyle: { color: C.ink, borderWidth: 2, borderColor: '#fff' }
                    }
                ]
            }
        }
    },
    watch: {
        visible(val) {
            this.innerVisible = val
            if (val) {
                this.days = this.initialDays
                this.load()
            }
        }
    },
    methods: {
        num(v) {
            return thousand(v)
        },
        money(v) {
            return thousand(v)
        },
        load() {
            if (!this.shop || !this.shop.shop_name) return
            this.loading = true
            statsApi
                .shopTrend(this.shop.shop_name, this.days)
                .then((data) => {
                    this.data = data || this.data
                })
                .catch(() => {
                    this.$message({ message: '店铺明细加载失败', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        },
        onClose() {
            this.$emit('update:visible', false)
        }
    }
}
</script>

<style scoped>
.drill-hd {
    display: flex;
    align-items: center;
}

.drill-hd__bar {
    width: 3px;
    height: 15px;
    border-radius: 2px;
    background: var(--csu-primary);
    margin-right: 9px;
}

.drill-hd__name {
    font-size: 15px;
    font-weight: 600;
    color: var(--csu-text);
}

.drill-hd__tag {
    margin-left: 10px;
    padding: 1px 7px;
    border-radius: 9px;
    font-size: 11px;
    color: var(--csu-primary-text);
    background: var(--csu-primary-bg);
}

.drill__kpis {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 16px;
}

.drill-kpi {
    background: var(--csu-panel-alt);
    border-radius: var(--csu-radius-sm);
    padding: 9px 11px;
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
}

.drill-kpi em {
    flex: 0 0 100%;
    font-style: normal;
    font-size: 11.5px;
    color: var(--csu-text-weak);
    margin-bottom: 3px;
}

.drill-kpi b {
    font-size: 18px;
    font-weight: 700;
    color: var(--csu-text);
}

.drill-kpi span {
    font-size: 11px;
    color: var(--csu-text-weak);
    margin-left: 3px;
}

.drill__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
}

.drill__section {
    font-size: 13px;
    font-weight: 600;
    color: var(--csu-text);
}

.drill__empty {
    height: 240px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--csu-text-weak);
}

.drill__empty i {
    font-size: 32px;
    color: var(--csu-icon-empty);
}

.drill__empty p {
    margin: 8px 0 0;
    font-size: 12.5px;
}

.drill__table {
    margin-top: 14px;
    border-top: 1px solid var(--csu-border-light);
    padding-top: 12px;
}
</style>
