<template>
    <chart-card title="订单状态构成" subtitle="oorder.checked 实时聚合 · 0 待派单 / 1 配送中 / 2 已完成"
        accent="#E0A020" height="300px" :loading="loading" :is-empty="isEmpty"
        empty-text="还没有任何订单" @retry="load">
        <template slot="tools">
            <span class="donut-total">共 <b class="csu-num">{{ fmt(total) }}</b> 单</span>
        </template>

        <div class="donut-wrap">
            <base-chart :option="option" height="300px" :visible="visible">
            </base-chart>
            <!-- 环形中心的总数用 DOM 叠层实现，不占用任何 ECharts 组件 -->
            <div class="donut-center">
                <b class="csu-num">{{ fmt(total) }}</b>
                <em>订单总数</em>
            </div>
        </div>

        <template slot="footer">
            <div class="donut-ft">
                <span>已送达率 <b class="csu-num">{{ deliveredRate }}</b></span>
                <span>在途待派 <b class="csu-num">{{ pendingRate }}</b></span>
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
 * 订单状态环形图。
 *
 * 颜色与列表页的状态标签严格对应（待派单=琥珀、配送中=主色橙、已完成=绿），
 * 这样"看板看到的颜色"和"订单列表里 tag 的颜色"是同一套语义，不用二次记忆。
 */
export default {
    name: 'StatusDonut',
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
        deliveredRate() {
            const done = this.items.filter((it) => it.status === 2).reduce((s, it) => s + Number(it.count || 0), 0)
            return this.total ? percent((done / this.total) * 100) : '0%'
        },
        pendingRate() {
            const wait = this.items.filter((it) => it.status !== 2).reduce((s, it) => s + Number(it.count || 0), 0)
            return this.total ? percent((wait / this.total) * 100) : '0%'
        },
        option() {
            const colorMap = { 0: C.amber, 1: C.primary, 2: C.green }
            const data = this.items.map((it) => ({
                name: it.label,
                value: Number(it.count || 0),
                itemStyle: { color: colorMap[it.status] || C.blue }
            }))

            return {
                tooltip: {
                    trigger: 'item',
                    formatter: (p) => {
                        return '<div style="font-weight:600;margin-bottom:4px">' + p.name + '</div>' +
                            '<div style="display:flex;justify-content:space-between;gap:16px">' +
                            '<span>' + p.marker + '订单数</span><b>' + thousand(p.value) + ' 单</b></div>' +
                            '<div style="display:flex;justify-content:space-between;gap:16px">' +
                            '<span style="opacity:.8">占比</span><b>' + p.percent + '%</b></div>'
                    }
                },
                legend: {
                    bottom: 0,
                    left: 'center',
                    itemWidth: 9,
                    itemHeight: 9,
                    textStyle: { color: C.textSub, fontSize: 12 }
                },
                series: [
                    {
                        type: 'pie',
                        radius: ['54%', '76%'],
                        center: ['50%', '43%'],
                        avoidLabelOverlap: true,
                        padAngle: 1.5,
                        itemStyle: { borderColor: C.panel, borderWidth: 3, borderRadius: 3 },
                        label: {
                            show: true,
                            formatter: '{b|{b}}\n{c|{c} 单 · {d}%}',
                            lineHeight: 17,
                            rich: {
                                b: { color: C.text, fontSize: 12, fontWeight: 600 },
                                c: { color: C.textWeak, fontSize: 11 }
                            }
                        },
                        labelLine: { length: 10, length2: 12, lineStyle: { color: C.border } },
                        emphasis: {
                            scale: true,
                            scaleSize: 5,
                            label: { show: true }
                        },
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
                .status()
                .then((data) => {
                    this.items = toList(data, data && data.items)
                })
                .catch(() => {
                    this.$message({ message: '订单状态数据加载失败', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
/* 图表 + 中心叠层：叠层不参与 ECharts 布局，只做视觉居中 */
.donut-wrap {
    position: relative;
    width: 100%;
}

.donut-center {
    position: absolute;
    left: 0;
    right: 0;
    top: 43%;
    transform: translateY(-50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    pointer-events: none;
    /* 压在 canvas 之上 */
    z-index: 2;
}

.donut-center b {
    font-size: 26px;
    font-weight: 700;
    color: var(--csu-text);
    line-height: 1.1;
}

.donut-center em {
    font-style: normal;
    font-size: 12px;
    color: var(--csu-text-weak);
    margin-top: 2px;
}

.donut-total {
    font-size: 12px;
    color: var(--csu-text-weak);
}

.donut-total b {
    color: var(--csu-text);
    font-weight: 700;
}

.donut-ft {
    display: flex;
    gap: 22px;
    font-size: 12.5px;
    color: var(--csu-text-sub);
}

.donut-ft b {
    color: var(--csu-text);
    font-weight: 700;
}
</style>
