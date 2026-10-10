<template>
    <chart-card title="下单时段分布" subtitle="oorder.create_time 取小时聚合 · 识别校园用餐双高峰"
        accent="#8C6BB1" height="320px" :loading="loading" :is-empty="isEmpty"
        empty-text="还没有可用于分析的订单时间" @retry="load">
        <template slot="tools">
            <el-tag v-if="!isEmpty" size="mini" effect="plain" class="hour-peak">
                高峰 {{ peakHour }}:00 · {{ fmt(peakCount) }} 单
            </el-tag>
        </template>

        <base-chart :option="option" height="320px" :visible="visible">
        </base-chart>

        <template slot="footer">
            <div class="hour-ft csu-hint">
                柱色示意：午市 11–13 点、晚市 17–19 点为校园点餐高峰时段，配餐与派单排班可据此倾斜。
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
 * 下单时段分布（0–23 时）。
 *
 * 这是本项目相对通用外卖后台"多加的一维"：校园场景的订单几乎全部集中在两个
 * 课间窗口，时段分布直接对应"备餐量 / 派单排班"，比单纯的日趋势更有运营含义。
 * 柱色按午市/晚市/其他分三档，不需要图例就能读出双峰结构。
 */
export default {
    name: 'HourBar',
    components: { ChartCard, BaseChart },
    props: {
        visible: { type: Boolean, default: true }
    },
    data() {
        return {
            loading: false,
            hours: [],
            counts: [],
            peakHour: 0,
            peakCount: 0
        }
    },
    computed: {
        isEmpty() {
            return !this.hours || this.hours.length === 0 || this.total === 0
        },
        total() {
            return (this.counts || []).reduce((s, v) => s + Number(v || 0), 0)
        },
        option() {
            const hours = this.hours || []
            const counts = this.counts || []
            const peak = Number(this.peakCount || 0)

            const colorOf = (h) => {
                if (h >= 11 && h <= 13) return C.primary
                if (h >= 17 && h <= 19) return C.amber
                return 'rgba(42,59,76,.34)'
            }

            return {
                grid: { top: 30, left: 8, right: 14, bottom: 4, containLabel: true },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(140,107,177,.08)' } },
                    formatter: (params) => {
                        if (!params || !params.length) return ''
                        const idx = params[0].dataIndex
                        const h = hours[idx]
                        const v = Number(counts[idx] || 0)
                        const share = this.total ? Math.round((v / this.total) * 1000) / 10 : 0
                        const band = (h >= 11 && h <= 13) ? '午市' : (h >= 17 && h <= 19) ? '晚市' : '非高峰'
                        return '<div style="font-weight:600;margin-bottom:4px">' + h + ':00 – ' + (h + 1) + ':00' +
                            ' <span style="opacity:.65;font-weight:400">' + band + '</span></div>' +
                            '<div style="display:flex;justify-content:space-between;gap:18px">' +
                            '<span>' + params[0].marker + '订单量</span><b>' + thousand(v) + ' 单</b></div>' +
                            '<div style="display:flex;justify-content:space-between;gap:18px">' +
                            '<span style="opacity:.82">占全天</span><b>' + share + '%</b></div>'
                    }
                },
                xAxis: {
                    type: 'category',
                    data: hours.map((h) => h + '时'),
                    axisLine: { lineStyle: { color: C.border } },
                    axisTick: { show: false },
                    axisLabel: { color: C.textSub, fontSize: 11, interval: 1 }
                },
                yAxis: {
                    type: 'value',
                    minInterval: 1,
                    axisLine: { show: false },
                    axisTick: { show: false },
                    axisLabel: { color: C.textSub, fontSize: 11 },
                    splitLine: { lineStyle: { color: C.borderLight } }
                },
                series: [
                    {
                        name: '订单量',
                        type: 'bar',
                        barWidth: '58%',
                        data: counts.map((v, i) => ({
                            value: Number(v || 0),
                            itemStyle: {
                                color: colorOf(hours[i]),
                                borderRadius: [3, 3, 0, 0]
                            }
                        })),
                        markLine: peak > 0
                            ? {
                                silent: true,
                                symbol: 'none',
                                lineStyle: { color: C.purple, type: 'dashed', width: 1 },
                                label: {
                                    formatter: '峰值 ' + thousand(peak),
                                    color: C.purple,
                                    fontSize: 11,
                                    position: 'insideEndTop'
                                },
                                data: [{ yAxis: peak }]
                            }
                            : undefined
                    }
                ]
            }
        }
    },
    created() {
        this.load()
    },
    methods: {
        fmt(v) {
            return thousand(v)
        },
        load() {
            this.loading = true
            statsApi
                .hours()
                .then((data) => {
                    this.hours = (data && data.hours) || []
                    this.counts = (data && data.order_counts) || []
                    this.peakHour = (data && data.peak_hour) || 0
                    this.peakCount = (data && data.peak_count) || 0
                })
                .catch(() => {
                    this.$message({ message: '时段分布数据加载失败', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
.hour-peak {
    color: var(--csu-primary-text) !important;
    background: var(--csu-primary-bg) !important;
    border-color: var(--csu-border-primary) !important;
}

.hour-ft {
    line-height: 1.6;
}
</style>
