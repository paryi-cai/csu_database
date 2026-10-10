<template>
    <page-panel title="历史配送" subtitle="wuliu.ended = 1 · 已结束的配送记录" accent="#2A3B4C">
        <template slot="actions">
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索订单号 / 顾客电话 / 配送员编号"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
        </template>

        <div class="summary">
            <div class="summary__item">
                <em>历史配送记录</em>
                <b class="csu-num">{{ total }}</b>
                <span>条</span>
            </div>
            <div class="summary__item">
                <em>涉及配送员</em>
                <b class="csu-num">{{ dispCount }}</b>
                <span>位</span>
            </div>
            <div class="summary__item">
                <em>人均完成</em>
                <b class="csu-num">{{ perDisp }}</b>
                <span>单</span>
            </div>
            <div class="summary__item">
                <em>配送员排行</em>
                <b class="summary__text">{{ topDisp }}</b>
            </div>
        </div>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border empty-text="暂无已结束的配送记录" max-height="calc(100vh - 340px)">
            <el-table-column prop="order_id" label="订单号" width="110" align="center"></el-table-column>
            <el-table-column label="配送状态" width="120" align="center">
                <template>
                    <status-tag :status="2" :labels="wuliuLabels"></status-tag>
                </template>
            </el-table-column>
            <el-table-column prop="cons_phone" label="顾客电话" width="160" align="center"></el-table-column>
            <el-table-column prop="disp_id" label="配送员编号" width="140" align="center"></el-table-column>
            <el-table-column label="送达时长" min-width="140" align="center">
                <template slot-scope="scope">
                    <span class="deliver-time">{{ text(scope.row.deliver_time) }}</span>
                </template>
            </el-table-column>
        </el-table>

        <div class="table-ft">
            <span class="csu-hint">
                共 {{ total }} 条历史记录<span v-if="keywordTrimmed">（已按「{{ keywordTrimmed }}」筛选）</span>
            </span>
            <el-pagination v-if="paged" background layout="total, prev, pager, next, jumper" :total="total"
                :page-size="pageSize" :current-page="currentPage" @current-change="handlePageChange">
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
 * 历史配送（wuliu.ended = 1）。
 *
 * 接口沿用原实现：GET /api/manager/wuliu?id=1。
 * 页面里的"配送员排行"是从当前筛选结果里现算的，用于给出一个轻量的横向对比。
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
            wuliuLabels: { '0': '在途', '1': '已结束', '2': '已完成' }
        }
    },
    computed: {
        filterFields() {
            return ['order_id', 'cons_phone', 'disp_id', 'deliver_time']
        },
        currentPage() {
            return this.page
        },
        dispCount() {
            const set = new Set()
            this.filtered.forEach((row) => row.disp_id && set.add(row.disp_id))
            return set.size
        },
        perDisp() {
            return this.dispCount ? (this.total / this.dispCount).toFixed(1) : '0.0'
        },
        /** 当前筛选结果里单量最多的配送员 */
        topDisp() {
            const counter = {}
            this.filtered.forEach((row) => {
                if (!row.disp_id) return
                counter[row.disp_id] = (counter[row.disp_id] || 0) + 1
            })
            const entries = Object.keys(counter).map((k) => [k, counter[k]])
            if (!entries.length) return '—'
            entries.sort((a, b) => b[1] - a[1])
            return entries[0][0] + '（' + entries[0][1] + ' 单）'
        }
    },
    methods: {
        text(v) {
            return fmtText(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/manager/wuliu?id=1')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                        this.clampPage()
                    }
                })
                .catch(() => this.$message({ message: '网络故障，历史配送加载失败', type: 'error' }))
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
    color: var(--csu-text-sub);
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
