<template>
    <page-panel title="待派单" subtitle="oorder.checked = 0 · 派发后由触发器 wuliu_insert 置为「配送中」" accent="#E8622C">
        <template slot="actions">
            <el-select v-model="filter.shop" size="small" clearable filterable placeholder="按店铺筛选"
                class="filter-select">
                <el-option v-for="name in shopOptions" :key="name" :label="name" :value="name"></el-option>
            </el-select>
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索订单号 / 店铺 / 顾客 / 电话"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
        </template>

        <!-- 摘要条：把"有多少单、多少钱"讲清楚，避免只看表格没有全局感 -->
        <div class="summary">
            <div class="summary__item">
                <em>待派单</em>
                <b class="csu-num">{{ total }}</b>
                <span>单</span>
            </div>
            <div class="summary__item">
                <em>订单金额合计</em>
                <b class="csu-num">{{ money(totalMoney) }}</b>
                <span>元</span>
            </div>
            <div class="summary__item">
                <em>涉及店铺</em>
                <b class="csu-num">{{ shopOptions.length }}</b>
                <span>家</span>
            </div>
            <div class="summary__item">
                <em>最新下单</em>
                <b class="summary__text">{{ latest }}</b>
            </div>
        </div>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border
            empty-text="没有待派单的订单（全部已派发或已被顾客取消）" max-height="calc(100vh - 340px)">
            <el-table-column prop="order_id" label="订单号" width="90" align="center"></el-table-column>
            <el-table-column prop="shop_name" label="店铺" min-width="140" align="center" show-overflow-tooltip>
            </el-table-column>
            <el-table-column label="金额（元）" width="110" align="right">
                <template slot-scope="scope">
                    <span class="csu-num money">{{ money(scope.row.price) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="订餐方式" width="110" align="center">
                <template slot-scope="scope">
                    <span class="way-chip">{{ scope.row.orderway }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="create_time" label="下单时间" width="170" align="center"></el-table-column>
            <el-table-column prop="cons_name" label="订餐人" width="100" align="center"></el-table-column>
            <el-table-column prop="cons_phone" label="联系电话" width="140" align="center"></el-table-column>
            <el-table-column prop="cons_addre" label="取餐地址" min-width="120" align="center"
                show-overflow-tooltip></el-table-column>
            <el-table-column label="操作" width="130" align="center" fixed="right">
                <template slot-scope="scope">
                    <el-button size="mini" type="primary" @click="show_dialog(scope.row)">派发</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="table-ft">
            <span class="csu-hint">
                <template v-if="hasFilter">已筛选出 {{ total }} 条</template>
                <template v-else>共 {{ total }} 条待派单订单</template>
            </span>
            <el-pagination v-if="paged" background layout="total, prev, pager, next, jumper" :total="total"
                :page-size="pageSize" :current-page="page" @current-change="handlePageChange">
            </el-pagination>
        </div>

        <el-dialog title="派发订单" :visible.sync="dialog" width="30%" :close-on-click-modal="false">
            <div class="dispatch-head">
                <span>订单 <b class="csu-num">#{{ form.order_id }}</b></span>
                <span class="dispatch-head__shop">{{ currentRow.shop_name }}</span>
                <span class="dispatch-head__addr">{{ currentRow.cons_addre }}</span>
            </div>
            <el-form ref="form" :model="form" label-width="120px">
                <el-form-item label="选择配送员：">
                    <el-select v-model="form.dispatcher_id" placeholder="请选择配送员编号" style="width: 100%">
                        <el-option v-for="(item, index) in disp_range" :key="index" :label="item.disp_id"
                            :value="item.disp_id"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="预计送货时间：">
                    <el-input v-model="form.deliver_time" placeholder="例如 30分钟"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="dialog = false">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="add()">确定派发</el-button>
            </div>
        </el-dialog>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import listPager from '@/mixins/listPager'
import { fmtMoney } from '@/utils/format'

/**
 * 待派单订单（原「未发货订单」）。
 *
 * 只改呈现层：接口仍是 GET /api/manager/unsend（同时返回 disp_range）与
 * POST /api/manager/unsend。订单状态由数据库触发器 wuliu_insert 负责置为 1，
 * 前端拿到成功响应后重新拉列表即可，不做任何本地状态推断。
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
            submitting: false,
            rawList: [],
            filter: { keyword: '', shop: '' },
            page: 1,
            pageSize: 10,
            dialog: false,
            disp_range: [],
            currentRow: {},
            form: {
                dispatcher_id: '',
                order_id: '',
                deliver_time: ''
            }
        }
    },
    computed: {
        filterFields() {
            return ['order_id', 'shop_name', 'cons_name', 'cons_phone', 'cons_addre', 'orderway', 'create_time']
        },
        hasFilter() {
            return !!(this.keywordTrimmed || this.filter.shop)
        },
        /** 关键字筛完之后再叠加"店铺"这一维 */
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
            return this.filtered.reduce((sum, row) => sum + Number(row.price || 0), 0)
        },
        /** 展示最新一单的下单时间：比"最早"更能反映当前待办的新鲜度 */
        latest() {
            const times = this.filtered.map((row) => row.create_time).filter(Boolean).sort()
            return times.length ? times[times.length - 1] : '—'
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
                .get('/api/manager/unsend')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                        this.disp_range = res.data.disp_range || []
                        this.clampPage()
                    }
                })
                .catch(() => this.$message({ message: '网络故障，待派单列表加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        },
        show_dialog(row) {
            this.currentRow = row
            this.form.order_id = row.order_id
            this.form.dispatcher_id = ''
            this.form.deliver_time = ''
            this.dialog = true
        },
        add() {
            if (!this.form.dispatcher_id) {
                this.$message({ message: '请选择配送员', type: 'warning' })
                return
            }
            this.submitting = true
            this.$axios
                .post('/api/manager/unsend', this.form)
                .then((res) => {
                    if (res.data.status == 200) {
                        this.$message({ message: res.data.msg || '派发成功', type: 'success' })
                        this.dialog = false
                        this.getdata()
                    } else {
                        this.$message({ message: res.data.msg || '派发失败', type: 'error' })
                    }
                })
                .catch(() => this.$message({ message: '网络故障，派发失败', type: 'error' }))
                .finally(() => {
                    this.submitting = false
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

.summary__text {
    font-size: 14px !important;
    font-weight: 600 !important;
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

.dispatch-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
    padding: 9px 12px;
    background: var(--csu-primary-bg);
    border-radius: var(--csu-radius-sm);
    font-size: 13px;
    color: var(--csu-primary-text);
}

.dispatch-head__shop,
.dispatch-head__addr {
    padding-left: 10px;
    border-left: 1px solid var(--csu-border-primary);
    color: var(--csu-text-sub);
}

@media (max-width: 1200px) {
    .summary {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}
</style>
