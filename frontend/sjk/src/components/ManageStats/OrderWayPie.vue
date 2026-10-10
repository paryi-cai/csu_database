<template>
    <chart-card title="订餐方式构成" subtitle="读 orderway 表（count 由数据库触发器维护）" accent="#3E7CB1" height="300px"
        :loading="loading" :is-empty="isEmpty" empty-text="orderway 表暂无统计记录" @retry="load">
        <template slot="tools">
            <span class="ow-total">合计 <b class="csu-num">{{ fmt(total) }}</b> 单</span>
        </template>

        <base-chart :option="option" height="300px" :visible="visible">
        </base-chart>

        <template slot="footer">
            <div class="ow-ft csu-hint">
                口径提示：本图直接读 orderway 表（触发器 order_insert / order_update / order_delete
                增量维护的历史累计值），与右侧按 oorder 实时聚合的图表口径不同。
            </div>
        </template>
    </chart-card>
</template>

<script>
import ChartCard from '@/components/common/ChartCard.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import { statsApi, toList } from '@/api/index'
import { C, thousand, percent } from '@/charts/echarts'

/**
 * 订餐方式占比饼图。
 *
 * 数据库职责边界：orderway.count 由触发器维护，Java 侧不重算，
 * 因此这个面板是"数据库既有统计"的直接呈现，与按 oorder 实时聚合的面板口径不同——
 * 页脚里显式写明这一点，避免看板被误解为数据不一致。
 */
export default {
    name: 'OrderWayPie',
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
        total() {
            return this.items.reduce((sum, it) => sum + Number(it.count || 0), 0)
        },
        isEmpty() {
            return this.total === 0
        },
        option() {
            const palette = [C.primary, C.blue, C.amber, C.purple]
            const data = this.items.map((it, i) => ({
                name: it.name,
                value: Number(it.count || 0),
                itemStyle: { color: palette[i % palette.length] }
            }))

            return {
                tooltip: {
                    trigger: 'item',
                    formatter: (p) => {
                        return '<div style="font-weight:600;margin-bottom:4px">' + p.name + '</div>' +
                            '<div style="display:flex;justify-content:space-between;gap:16px">' +
                            '<span>' + p.marker + '累计单量</span><b>' + thousand(p.value) + ' 单</b></div>' +
                            '<div style="display:flex;justify-content:space-between;gap:16px">' +
                            '<span style="opacity:.8">占比</span><b>' + p.percent + '%</b></div>'
                    }
                },
                legend: {
                    bottom: 0,
                    left: 'center',
                    itemWidth: 9,
                    itemHeight: 9,
                    textStyle: { color: C.textSub, fontSize: 12 },
                    formatter: (name) => {
                        const row = this.items.find((it) => it.name === name)
                        if (!row) return name
                        return name + '  ' + percent(Number(row.ratio || 0))
                    }
                },
                series: [
                    {
                        type: 'pie',
                        radius: ['42%', '68%'],
                        center: ['50%', '44%'],
                        padAngle: 2,
                        itemStyle: { borderColor: C.panel, borderWidth: 3, borderRadius: 4 },
                        label: {
                            show: true,
                            formatter: '{b|{b}}\n{c|{d}%}',
                            lineHeight: 17,
                            rich: {
                                b: { color: C.text, fontSize: 12, fontWeight: 600 },
                                c: { color: C.textWeak, fontSize: 11 }
                            }
                        },
                        labelLine: { length: 10, length2: 12, lineStyle: { color: C.border } },
                        emphasis: { scale: true, scaleSize: 5 },
                        data: data
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
                .orderway()
                .then((data) => {
                    this.items = toList(data, data && data.items)
                })
                .catch(() => {
                    this.$message({ message: '订餐方式数据加载失败', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
.ow-total {
    font-size: 12px;
    color: var(--csu-text-weak);
}

.ow-total b {
    color: var(--csu-text);
    font-weight: 700;
}

.ow-ft {
    line-height: 1.6;
}
</style>
