<template>
    <page-panel title="在途配送" subtitle="wuliu.ended = 0 · 物流记录与对应订单当前均为「配送中」" accent="#E8622C">
        <template slot="actions">
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索订单号 / 顾客电话 / 配送员编号"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
        </template>

        <div class="summary">
            <div class="summary__item">
                <em>在途配送记录</em>
                <b class="csu-num">{{ total }}</b>
                <span>条</span>
            </div>
            <div class="summary__item">
                <em>涉及配送员</em>
                <b class="csu-num">{{ dispCount }}</b>
                <span>位</span>
            </div>
            <div class="summary__item">
                <em>人均在途</em>
                <b class="csu-num">{{ perDisp }}</b>
                <span>单</span>
            </div>
            <div class="summary__item">
                <em>参考配送时长</em>
                <b class="summary__text">{{ timeHint }}</b>
            </div>
        </div>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border
            empty-text="当前没有在途配送记录（派发订单后这里会出现记录）" max-height="calc(100vh - 340px)">
            <el-table-column prop="order_id" label="订单号" width="110" align="center"></el-table-column>
            <el-table-column label="配送状态" width="120" align="center">
                <template>
                    <status-tag :status="0" :labels="wuliuLabels"></status-tag>
                </template>
            </el-table-column>
            <el-table-column prop="cons_phone" label="顾客电话" width="160" align="center"></el-table-column>
            <el-table-column prop="disp_id" label="配送员编号" width="140" align="center"></el-table-column>
            <el-table-column label="预计送达" min-width="140" align="center">
                <template slot-scope="scope">
                    <span class="deliver-time">{{ text(scope.row.deliver_time) }}</span>
                </template>
            </el-table-column>
        </el-table>

        <div class="table-ft">
            <span class="csu-hint">
                共 {{ total }} 条在途记录<span v-if="keywordTrimmed">（已按「{{ keywordTrimmed }}」筛选）</span>
            </span>
            <el-pagination v-if="paged" background layout="total, prev, pager, next, jumper" :total="total"
                :page-size="pageSize" :current-page="page" @current-change="handlePageChange">
            </el-pagination>
        </div>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import listPager from '@/mixins/listPager'
import { fmtText } from '@/utils/format'

/**
 * 在途配送（wuliu.ended = 0）。
 *
 * 接口沿用原实现：GET /api/manager/wuliu?id=0（响应不含 ended 字段，
 * 因此这里用"在途"标签直接表达语义，而不是去读一个不存在的字段）。
 */
export default {
    components: { PagePanel, StatusTag },
    mixins: [listPager],
    created() {
        this.getdata()
    },
    data() {
        return {
            loading: false,
            rawList: [],
            filter: { keyword: '' },
            page: 1,
            pageSize: 10,
            /** 物流的 ended 语义与订单 checked 不同，这里单独给文案 */
            wuliuLabels: { '0': '在途', '1': '已结束' }
        }
    },
    computed: {
        filterFields() {
            return ['order_id', 'cons_phone', 'disp_id', 'deliver_time']
        },
        dispCount() {
            const set = new Set()
            this.filtered.forEach((row) => row.disp_id && set.add(row.disp_id))
            return set.size
        },
        perDisp() {
            return this.dispCount ? (this.total / this.dispCount).toFixed(1) : '0.0'
        },
        /**
         * deliver_time 是「20分钟」这类自由文本（varchar），不是时间戳，
         * 无法聚合排序，这里取当前筛选结果里出现最多的一种作为参考值。
         */
        timeHint() {
            const counter = {}
            this.filtered.forEach((row) => {
                if (!row.deliver_time) return
                counter[row.deliver_time] = (counter[row.deliver_time] || 0) + 1
            })
            const keys = Object.keys(counter)
            if (!keys.length) return '—'
            keys.sort((a, b) => counter[b] - counter[a])
            return keys[0]
        }
    },
    methods: {
        text(v) {
            return fmtText(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/manager/wuliu?id=0')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                        this.clampPage()
                    }
                })
                .catch(() => this.$message({ message: '网络故障，在途配送加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
.filter-input {
    width: 280px;
}

.summary {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 14px;
}

.summary__item {
    background: var(--csu-panel-alt);
    border-radius: var(--csu-radius-sm);
    padding: 10px 12px;
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
}

.summary__item em {
    flex: 0 0 100%;
    font-style: normal;
    font-size: 11.5px;
    color: var(--csu-text-weak);
    margin-bottom: 3px;
}

.summary__item b {
    font-size: 19px;
    font-weight: 700;
    color: var(--csu-text);
}

.summary__item span {
    font-size: 11px;
    color: var(--csu-text-weak);
    margin-left: 3px;
}

.summary__text {
    font-size: 14px !important;
    font-weight: 600 !important;
}

.deliver-time {
    color: var(--csu-primary-text);
    font-weight: 600;
}

.table-ft {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 14px;
    flex-wrap: wrap;
}

@media (max-width: 1200px) {
    .summary {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}
</style>
