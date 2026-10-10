<template>
    <chart-card title="订单量与营业额走势" :subtitle="subtitle" accent="#E8622C" height="320px" :loading="loading"
        :is-empty="isEmpty" empty-text="所选区间内没有订单记录" @retry="load">
        <template slot="tools">
            <el-radio-group v-model="days" size="mini" @change="onDaysChange">
                <el-radio-button :label="7">近 7 天</el-radio-button>
                <el-radio-button :label="30">近 30 天</el-radio-button>
            </el-radio-group>
        </template>

        <base-chart :option="option" height="320px" :visible="visible">
        </base-chart>

        <template slot="footer">
            <div class="trend-ft">
                <span>区间订单 <b class="csu-num">{{ fmt(data.order_total) }}</b> 单</span>
                <span>区间营业额 <b class="csu-num">{{ fmt(data.revenue_total) }}</b> 元</span>
                <span>日均 <b class="csu-num">{{ fmt(data.order_avg, 1) }}</b> 单 /
                    <b class="csu-num">{{ fmt(data.revenue_avg, 1) }}</b> 元</span>
                <span class="trend-ft__tip">左轴=订单量（面积），右轴=营业额（折线）</span>
            </div>
        </template>
    </chart-card>
</template>

<script>
import ChartCard from '@/components/common/ChartCard.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import { statsApi } from '@/api/index'
import { C, thousand } from '@/charts/echarts'

/**
 * 订单量 + 营业额双 Y 轴走势。
 *
 * 口径：来自 oorder 表按 create_time（varchar，'yyyy-MM-dd HH:mm:ss'）的日期部分聚合；
 *      服务端会把区间内没有订单的日期补零，保证折线连续不跳。
 *
 * 双轴设计：订单量走面积（左轴，视觉上是"底盘"），营业额走折线（右轴，视觉上更"尖"），
 * 两者量级不同但趋势同向时能明显看出"客单价"的变化——这是单轴叠图看不到的信息。
 */
export default {
    name: 'TrendChart',
    components: { ChartCard, BaseChart },
    props: {
        visible: { type: Boolean, default: true },
        /** 初始区间：7 | 30 */
        initialDays: { type: Number, default: 7 }
    },
    data() {
        return {
            days: this.initialDays,
            loading: false,
            data: {
                dates: [],
                order_counts: [],
                revenues: [],
                order_total: 0,
                revenue_total: 0,
                order_avg: 0,
                revenue_avg: 0
            }
        }
    },
    computed: {
        subtitle() {
            return 'oorder 按天聚合 · 服务端补零 · 当前区间 ' + this.days + ' 天'
        },
        isEmpty() {
            return !this.data.dates || this.data.dates.length === 0
        },
        option() {
            const dates = this.data.dates || []
            const counts = this.data.order_counts || []
            const revenues = this.data.revenues || []
            // x 轴标签抽稀：30 天时只显示 1/5/10/15/20/25/30 与首尾，避免挤成一片
            const interval = dates.length > 10 ? Math.ceil(dates.length / 7) : 0

            return {
                grid: { top: 34, left: 8, right: 8, bottom: 4, containLabel: true },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'line', lineStyle: { color: C.primaryLight, width: 1, type: 'dashed' } },
                    formatter: (params) => {
                        if (!params || !params.length) return ''
                        const head = '<div style="font-weight:600;margin-bottom:4px">' + params[0].axisValue + '</div>'
                        return head + params.map((p) => {
                            const unit = p.seriesName === '营业额' ? ' 元' : ' 单'
                            return '<div style="display:flex;justify-content:space-between;gap:16px">' +
                                '<span>' + p.marker + p.seriesName + '</span>' +
                                '<b>' + thousand(p.value) + unit + '</b></div>'
                        }).join('')
                    }
                },
                legend: { show: false },
                xAxis: {
                    type: 'category',
                    boundaryGap: false,
                    data: dates,
                    axisLabel: { interval: interval, color: C.textSub, fontSize: 11 },
                    axisLine: { lineStyle: { color: C.border } },
                    axisTick: { show: false }
                },
                yAxis: [
                    {
                        type: 'value',
                        name: '订单量',
                        nameTextStyle: { color: C.textWeak, fontSize: 11, padding: [0, 0, 6, 0] },
                        minInterval: 1,
                        axisLine: { show: false },
                        axisTick: { show: false },
                        axisLabel: { color: C.textSub, fontSize: 11 },
                        splitLine: { lineStyle: { color: C.borderLight } }
                    },
                    {
                        type: 'value',
                        name: '营业额',
                        nameTextStyle: { color: C.textWeak, fontSize: 11, padding: [0, 0, 6, 0] },
                        axisLine: { show: false },
                        axisTick: { show: false },
                        axisLabel: { color: C.textSub, fontSize: 11, formatter: '{value}' },
                        splitLine: { show: false }
                    }
                ],
                series: [
                    {
                        name: '订单量',
                        type: 'line',
                        yAxisIndex: 0,
                        data: counts,
                        smooth: true,
                        symbol: 'circle',
                        symbolSize: 6,
                        showSymbol: counts.length <= 12,
                        lineStyle: { width: 2.6, color: C.primary },
                        itemStyle: { color: C.primary, borderWidth: 2, borderColor: '#fff' },
                        areaStyle: {
                            color: {
                                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                                colorStops: [
                                    { offset: 0, color: 'rgba(232,98,44,.26)' },
                                    { offset: 1, color: 'rgba(232,98,44,.02)' }
                                ]
                            }
                        }
                    },
                    {
                        name: '营业额',
                        type: 'line',
                        yAxisIndex: 1,
                        data: revenues,
                        smooth: true,
                        symbol: 'circle',
                        symbolSize: 6,
                        showSymbol: revenues.length <= 12,
                        lineStyle: { width: 2.2, color: C.ink, type: 'solid' },
                        itemStyle: { color: C.ink, borderWidth: 2, borderColor: '#fff' }
                    }
                ]
            }
        }
    },
    created() {
        this.load()
    },
    methods: {
        fmt(v, d) {
            return thousand(v, d || 0)
        },
        /**
         * 切换 7/30 天。
         *
         * 除了重新取数，还要把区间**上报给父组件**：KPI 卡的"近 N 天日均"文案依赖它，
         * 下钻抽屉也用同一个区间。原来这里直接绑 load，父组件的 activeDays 永远是 7，
         * 于是切到 30 天后 KPI 卡还在说"近 7 天日均"——文案与图表口径不一致。
         */
        onDaysChange(val) {
            this.$emit('days-change', val)
            this.load()
        },
        load() {
            this.loading = true
            statsApi
                .trend(this.days)
                .then((data) => {
                    this.data = data || this.data
                })
                .catch(() => {
                    this.$message({ message: '走势数据加载失败', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
.trend-ft {
    display: flex;
    align-items: center;
    gap: 22px;
    flex-wrap: wrap;
    font-size: 12.5px;
    color: var(--csu-text-sub);
}

.trend-ft b {
    color: var(--csu-text);
    font-weight: 700;
    margin: 0 1px;
}

.trend-ft__tip {
    margin-left: auto;
    color: var(--csu-text-weak);
    font-size: 11.5px;
}
</style>
