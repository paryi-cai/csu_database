<template>
    <chart-card title="店铺业绩榜" subtitle="oorder 按店铺聚合 · 点击柱子查看该店走势" accent="#2A3B4C" height="320px" :loading="loading"
        :is-empty="isEmpty" empty-text="所选区间内没有店铺业绩数据" @retry="load">
        <template slot="tools">
            <el-radio-group v-model="metric" size="mini" @change="load">
                <el-radio-button label="money">按营业额</el-radio-button>
                <el-radio-button label="count">按订单量</el-radio-button>
            </el-radio-group>
            <el-select v-model="limit" size="mini" style="width: 92px" @change="load">
                <el-option :value="5" label="Top 5"></el-option>
                <el-option :value="8" label="Top 8"></el-option>
                <el-option :value="12" label="Top 12"></el-option>
            </el-select>
        </template>

        <base-chart :option="option" height="320px" :visible="visible" @chart-click="onBarClick"></base-chart>
    </chart-card>
</template>

<script>
import ChartCard from '@/components/common/ChartCard.vue'
import BaseChart from '@/components/common/BaseChart.vue'
import { statsApi, toList } from '@/api/index'
import { C, thousand } from '@/charts/echarts'

/**
 * 店铺业绩 Top N 横向条形图（支持下钻）。
 *
 * 横向而非纵向的原因：店铺名是中文长文本（"小美负一楼蛋炒饭"），
 * 纵向柱状图的 x 轴标签会挤成两行或被迫旋转，可读性很差。
 *
 * 配色：第 1 名用主色实心，第 2、3 名用主色降透明度，其余用墨蓝——
 * 一眼能看出"头部三家"是谁，但不引入第三、第四种颜色把图表搞花。
 */
export default {
    name: 'ShopRankChart',
    components: { ChartCard, BaseChart },
    props: {
        visible: { type: Boolean, default: true }
    },
    data() {
        return {
            metric: 'money',
            limit: 8,
            loading: false,
            items: []
        }
    },
    computed: {
        isEmpty() {
            return !this.items || this.items.length === 0
        },
        isMoney() {
            return this.metric === 'money'
        },
        option() {
            // 横向条形图从下往上画，因此倒序后第 1 名才在最上方
            const rows = (this.items || []).slice().reverse()
            const names = rows.map((it) => it.shop_name)
            const values = rows.map((it) => (this.isMoney ? Number(it.revenue || 0) : Number(it.order_count || 0)))

            return {
                grid: { top: 12, left: 8, right: 58, bottom: 6, containLabel: true },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(232,98,44,.06)' } },
                    formatter: (params) => {
                        if (!params || !params.length) return ''
                        const row = rows[params[0].dataIndex] || {}
                        return '<div style="font-weight:600;margin-bottom:5px">' + row.shop_name + '</div>' +
                            this.line('营业额', thousand(row.revenue) + ' 元') +
                            this.line('订单量', thousand(row.order_count) + ' 单') +
                            this.line('单价', thousand(row.price) + ' 元') +
                            this.line('数据库销量(m_sale_v)', thousand(row.sale) + ' 单')
                    }
                },
                xAxis: {
                    type: 'value',
                    axisLine: { show: false },
                    axisTick: { show: false },
                    axisLabel: { color: C.textSub, fontSize: 11 },
                    splitLine: { lineStyle: { color: C.borderLight } }
                },
                yAxis: {
                    type: 'category',
                    data: names,
                    axisLine: { lineStyle: { color: C.border } },
                    axisTick: { show: false },
                    axisLabel: {
                        color: C.text,
                        fontSize: 12,
                        width: 108,
                        overflow: 'truncate'
                    }
                },
                series: [
                    {
                        name: this.isMoney ? '营业额' : '订单量',
                        type: 'bar',
                        barWidth: 14,
                        data: rows.map((it, idx) => {
                            const rank = rows.length - idx
                            let color = C.ink
                            if (rank === 1) color = C.primary
                            else if (rank === 2) color = 'rgba(232,98,44,.72)'
                            else if (rank === 3) color = 'rgba(232,98,44,.48)'
                            return { value: values[idx], itemStyle: { color: color, borderRadius: [0, 4, 4, 0] } }
                        }),
                        label: {
                            show: true,
                            position: 'right',
                            distance: 6,
                            color: C.textSub,
                            fontSize: 11,
                            fontFamily: 'Consolas, ui-monospace, monospace',
                            formatter: (p) => thousand(p.value)
                        },
                        emphasis: { itemStyle: { color: C.primary } },
                        cursor: 'pointer'
                    }
                ]
            }
        }
    },
    created() {
        this.load()
    },
    methods: {
        line(label, value) {
            return '<div style="display:flex;justify-content:space-between;gap:18px">' +
                '<span style="opacity:.82">' + label + '</span><b>' + value + '</b></div>'
        },
        load() {
            this.loading = true
            statsApi
                .shops(this.limit, this.metric)
                .then((data) => {
                    this.items = toList(data, data && data.items)
                })
                .catch(() => {
                    this.$message({ message: '店铺业绩数据加载失败', type: 'error' })
                })
                .finally(() => {
                    this.loading = false
                })
        },
        onBarClick(evt) {
            const row = (this.items || []).find((it) => it.shop_name === evt.name)
            if (row) this.$emit('drill', row)
        }
    }
}
</script>
