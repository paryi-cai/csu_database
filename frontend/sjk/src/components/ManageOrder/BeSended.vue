<template>
    <page-panel title="已完成" subtitle="视图 sended_order · checked = 2 且物流已结束" accent="#2FA36B">
        <template slot="actions">
            <el-select v-model="filter.shop" size="small" clearable filterable placeholder="按店铺筛选"
                class="filter-select">
                <el-option v-for="name in shopOptions" :key="name" :label="name" :value="name"></el-option>
            </el-select>
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索订单号 / 店铺 / 顾客 / 配送员"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
        </template>

        <div class="summary">
            <div class="summary__item">
                <em>已完成订单</em>
                <b class="csu-num">{{ total }}</b>
                <span>单</span>
            </div>
            <div class="summary__item">
                <em>已完成金额</em>
                <b class="csu-num">{{ money(totalMoney) }}</b>
                <span>元</span>
            </div>
            <div class="summary__item">
                <em>单均金额</em>
                <b class="csu-num">{{ money(avgMoney) }}</b>
                <span>元</span>
            </div>
            <div class="summary__item">
                <em>参与配送的配送员</em>
                <b class="csu-num">{{ dispCount }}</b>
                <span>位</span>
            </div>
        </div>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border empty-text="还没有已完成的订单" max-height="calc(100vh - 340px)">
            <el-table-column prop="order_id" label="订单号" width="90" align="center"></el-table-column>
            <el-table-column prop="shop_name" label="店铺" min-width="140" align="center" show-overflow-tooltip>
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
            <el-table-column prop="cons_phone" label="联系电话" width="140" align="center"></el-table-column>
            <el-table-column prop="cons_addre" label="取餐地址" min-width="110" align="center"
                show-overflow-tooltip></el-table-column>
            <el-table-column prop="disp_id" label="配送员编号" width="120" align="center"></el-table-column>
            <el-table-column prop="deliver_time" label="实际送达" width="110" align="center"></el-table-column>
        </el-table>

        <div class="table-ft">
            <span class="csu-hint">
                <template v-if="hasFilter">已筛选出 {{ total }} 条</template>
                <template v-else>共 {{ total }} 条已完成订单</template>
            </span>
            <el-pagination v-if="paged" background layout="total, prev, pager, next, jumper" :total="total"
                :page-size="pageSize" :current-page="page" @current-change="handlePageChange">
            </el-pagination>
        </div>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import listPager from '@/mixins/listPager'
import { fmtMoney } from '@/utils/format'

/**
 * 已完成订单。
 *
 * 数据来自数据库视图 sended_order（checked = 2 且物流已结束），接口不变：
 * GET /api/manager/sended。本页只加呈现层与前端筛选/分页。
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
            filter: { keyword: '', shop: '' },
            page: 1,
            pageSize: 10
        }
    },
    computed: {
        filterFields() {
            return ['order_id', 'shop_name', 'cons_name', 'cons_phone', 'cons_addre', 'disp_id', 'order_way']
        },
        hasFilter() {
            return !!(this.keywordTrimmed || this.filter.shop)
        },
        filtered() {
            const base = this.applyKeywordFilter()
            const shop = this.filter.shop
            if (!shop) return base
            return base.filter((row) => row.shop_name === shop)
        },
        shopOptions() {
            const set = new Set()
            this.rawList.forEach((row) => row.shop_name && set.add(row.shop_name))
            return Array.from(set).sort()
        },
        totalMoney() {
            return this.filtered.reduce((sum, row) => sum + Number(row.order_money || 0), 0)
        },
        avgMoney() {
            return this.total ? this.totalMoney / this.total : 0
        },
        dispCount() {
            const set = new Set()
            this.filtered.forEach((row) => row.disp_id && set.add(row.disp_id))
            return set.size
        }
    },
    watch: {
        /**
         * 切换「按店铺」筛选后回到第 1 页。
         *
         * mixin 只 watch 了关键字，而 clampPage() 要等请求回来才执行；
         * 若停在第 5 页再筛一个只有几条订单的店铺，页码不会自动回退（分页器是单向
         * :current-page 绑定），表格会直接空掉。
         */
        'filter.shop'() {
            this.page = 1
        }
    },
    methods: {
        money(v) {
            return fmtMoney(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/manager/sended')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                        this.clampPage()
                    }
                })
                .catch(() => this.$message({ message: '网络故障，已完成列表加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        }
    }
}
</script>

<style scoped>
.filter-input {
    width: 260px;
}

.filter-select {
    width: 170px;
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
