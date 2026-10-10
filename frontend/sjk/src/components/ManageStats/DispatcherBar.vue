<template>
    <chart-card title="配送员工作量对比" subtitle="wuliu 按 disp_id 聚合 · 关联 dispatcher 取姓名" accent="#2FA36B" height="320px"
        :loading="loading" :is-empty="isEmpty" empty-text="还没有任何派发记录" @retry="load">
        <template slot="tools">
            <span class="dp-total">
                共 <b class="csu-num">{{ fmt(totalSending) }}</b> 在途 /
                <b class="csu-num">{{ fmt(totalSended) }}</b> 已完成
            </span>
        </template>

        <base-chart :option="option" height="320px" :visible="visible">
        </base-chart>
    </chart-card>
</template>

<script>
import ChartCard from '@/components/common/ChartCard.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import { statsApi, toList } from '@/api/index'
import { C, thousand } from '@/charts/echarts'

/**
 * 配送员接单量对比（堆叠柱状图）。
 *
 * 拆「在途 / 已完成」两段而不是只画总量：总量相同的两个人，一个是手上压了一堆
 * 没送完的、一个是全部送达的，管理含义完全不同。段色沿用全站语义色
 * （在途=主色橙、已完成=绿），与订单/物流页的标签颜色一致。
 */
export default {
    name: 'DispatcherBar',
    components: { ChartCard, BaseChart },
    props: {
        visible: { type: Boolean, default: true }
    },
    data() {
        return {
            loading: false,
            items: []
        }
    },
    computed: {
        isEmpty() {
            return !this.items || this.items.length === 0
        },
        totalSending() {
            return this.items.reduce((s, it) => s + Number(it.sending_count || 0), 0)
        },
        totalSended() {
            return this.items.reduce((s, it) => s + Number(it.sended_count || 0), 0)
        },
        option() {
            const rows = this.items || []
            const names = rows.map((it) => it.disp_name || it.disp_id)
            const sending = rows.map((it) => Number(it.sending_count || 0))
            const sended = rows.map((it) => Number(it.sended_count || 0))

            return {
                grid: { top: 40, left: 8, right: 12, bottom: 4, containLabel: true },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(47,163,107,.07)' } },
                    formatter: (params) => {
                        if (!params || !params.length) return ''
                        const row = rows[params[0].dataIndex] || {}
                        const total = Number(row.total || 0)
                        const done = Number(row.sended_count || 0)
                        const rate = total ? Math.round((done / total) * 1000) / 10 : 0
                        return '<div style="font-weight:600;margin-bottom:5px">' + (row.disp_name || row.disp_id) +
                            ' <span style="opacity:.65;font-weight:400">' + row.disp_id + '</span></div>' +
                            params.map((p) => '<div style="display:flex;justify-content:space-between;gap:18px">' +
                                '<span>' + p.marker + p.seriesName + '</span><b>' + thousand(p.value) + ' 单</b></div>').join('') +
                            '<div style="display:flex;justify-content:space-between;gap:18px;margin-top:3px;padding-top:3px;border-top:1px solid rgba(255,255,255,.18)">' +
                            '<span style="opacity:.82">送达率</span><b>' + rate + '%</b></div>'
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
                    data: names,
                    axisLine: { lineStyle: { color: C.border } },
                    axisTick: { show: false },
                    axisLabel: {
                        color: C.text,
                        fontSize: 12,
                        interval: 0,
                        width: 76,
                        overflow: 'truncate'
                    }
                },
                yAxis: {
                    type: 'value',
                    minInterval: 1,
                    name: '单量',
                    nameTextStyle: { color: C.textWeak, fontSize: 11 },
                    axisLine: { show: false },
                    axisTick: { show: false },
                    axisLabel: { color: C.textSub, fontSize: 11 },
                    splitLine: { lineStyle: { color: C.borderLight } }
                },
                series: [
                    {
                        name: '在途',
                        type: 'bar',
                        stack: 'workload',
                        barWidth: 26,
                        data: sending,
                        itemStyle: { color: C.primary, borderRadius: [0, 0, 0, 0] },
                        emphasis: { itemStyle: { color: C.primaryLight } }
                    },
                    {
                        name: '已完成',
                        type: 'bar',
                        stack: 'workload',
                        barWidth: 26,
                        data: sended,
                        itemStyle: { color: C.green, borderRadius: [4, 4, 0, 0] },
                        label: {
                            show: true,
                            position: 'top',
                            color: C.textSub,
                            fontSize: 11,
                            fontFamily: 'Consolas, ui-monospace, monospace',
                            formatter: (p) => {
                                const row = rows[p.dataIndex] || {}
                                return thousand(row.total || 0)
                            }
                        },
                        emphasis: { itemStyle: { color: '#3FC184' } }
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
                .dispatchers()
                .then((data) => {
                    // 本接口的 data 直接是数组，用 toList 统一取，避免各处猜结构
                    this.items = toList(data, data)
                })
                .catch(() => {
                    this.$message({ message: '配送员数据加载失败', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
.dp-total {
    font-size: 12px;
    color: var(--csu-text-weak);
}

.dp-total b {
    color: var(--csu-text);
    font-weight: 700;
}
</style>
