<template>
    <page-panel title="配送中" subtitle="视图 sending_order · 已派发，正在送餐途中" accent="#E0A020">
        <template slot="actions">
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索店铺 / 配送员 / 地址"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
        </template>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border empty-text="没有配送中的订单" max-height="calc(100vh - 340px)">
            <el-table-column prop="shop_name" label="店铺" min-width="150" align="center" show-overflow-tooltip>
            </el-table-column>
            <el-table-column label="金额（元）" width="110" align="right">
                <template slot-scope="scope">
                    <span class="csu-num money">{{ money(scope.row.order_money) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="订餐方式" width="110" align="center">
                <template slot-scope="scope">
                    <span class="way-chip">{{ scope.row.order_way }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="cons_name" label="订餐人" width="100" align="center"></el-table-column>
            <el-table-column prop="cons_addre" label="取餐地址" min-width="120" align="center"
                show-overflow-tooltip></el-table-column>
            <el-table-column prop="disp_id" label="配送员编号" width="120" align="center"></el-table-column>
            <el-table-column prop="disp_phone" label="配送员电话" width="140" align="center"></el-table-column>
            <el-table-column label="预计送达" width="110" align="center">
                <template slot-scope="scope">
                    <span class="deliver-time">{{ text(scope.row.deliver_time) }}</span>
                </template>
            </el-table-column>
        </el-table>

        <div class="table-ft">
            <span class="csu-hint">
                共 {{ total }} 条配送中订单<span v-if="keywordTrimmed">（已按「{{ keywordTrimmed }}」筛选）</span>
                · 数据来自数据库视图 sending_order
            </span>
            <el-pagination v-if="paged" background layout="total, prev, pager, next" :total="total"
                :page-size="pageSize" :current-page="page" @current-change="handlePageChange">
            </el-pagination>
        </div>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import listPager from '@/mixins/listPager'
import { fmtMoney, fmtText } from '@/utils/format'

/**
 * 用户端 - 配送中订单。
 *
 * 接口不变：GET /api/user/sending（数据来自视图 sending_order，
 * 视图内含三表连接与 checked=1 过滤，前端不做任何状态判断）。
 * 注意视图列 dispatcher_phone 在响应中叫 disp_phone。
 */
export default {
    components: { PagePanel },
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
            pageSize: 10
        }
    },
    computed: {
        filterFields() {
            return ['shop_name', 'cons_name', 'cons_addre', 'disp_id', 'disp_phone', 'order_way']
        }
    },
    methods: {
        money(v) {
            return fmtMoney(v)
        },
        text(v) {
            return fmtText(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/user/sending')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                        this.clampPage()
                    }
                })
                .catch(() => this.$message({ message: '网络故障，订单加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
.filter-input {
    width: 220px;
}

.money {
    font-weight: 600;
    color: var(--csu-primary-text);
}

.way-chip {
    display: inline-block;
    padding: 1px 8px;
    border-radius: 10px;
    font-size: 12px;
    color: var(--csu-text-sub);
    background: var(--csu-panel-alt);
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
</style>
