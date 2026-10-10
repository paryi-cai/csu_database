<template>
    <page-panel title="待派单" subtitle="oorder.checked = 0 · 未派发前可修改收货信息或取消订单" accent="#E8622C">
        <template slot="actions">
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索店铺 / 姓名 / 地址"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
        </template>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border
            empty-text="没有待派单的订单，去「逛店铺」点一单吧" max-height="calc(100vh - 340px)">
            <el-table-column prop="shop_name" label="店铺" min-width="150" align="center" show-overflow-tooltip>
            </el-table-column>
            <el-table-column label="金额（元）" width="110" align="right">
                <template slot-scope="scope">
                    <span class="csu-num money">{{ money(scope.row.price) }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="create_time" label="下单时间" width="170" align="center"></el-table-column>
            <el-table-column label="订餐方式" width="110" align="center">
                <template slot-scope="scope">
                    <span class="way-chip">{{ scope.row.orderway }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="cons_name" label="订餐人" width="100" align="center"></el-table-column>
            <el-table-column prop="cons_addre" label="取餐地址" min-width="120" align="center"
                show-overflow-tooltip></el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
                <template slot-scope="scope">
                    <el-button size="mini" type="success" plain @click="showdia_ch(scope.row)">修改</el-button>
                    <el-button size="mini" type="danger" plain @click="showdia_dl(scope.row)">取消订单</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="table-ft">
            <span class="csu-hint">
                共 {{ total }} 条待派单订单<span v-if="keywordTrimmed">（已按「{{ keywordTrimmed }}」筛选）</span>
            </span>
            <el-pagination v-if="paged" background layout="total, prev, pager, next" :total="total"
                :page-size="pageSize" :current-page="page" @current-change="handlePageChange">
            </el-pagination>
        </div>

        <el-dialog title="修改订单" :visible.sync="dialog_chnage" width="30%" :close-on-click-modal="false">
            <el-form ref="form" :model="form_change" label-width="100px">
                <el-form-item label="订餐人姓名：">
                    <el-input v-model="form_change.cons_name"></el-input>
                </el-form-item>
                <el-form-item label="取餐地址：">
                    <el-input v-model="form_change.cons_addre"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="dialog_chnage = false">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="change">确认修改</el-button>
            </div>
        </el-dialog>

        <el-dialog title="取消订单" :visible.sync="dialog_delete" width="30%">
            <div class="confirm-body">
                确定取消订单
                <b>#{{ delete_id }}</b>
                吗？取消后店铺销量与订餐方式计数会由数据库触发器自动回退。
            </div>
            <div slot="footer">
                <el-button size="small" @click="dialog_delete = false">再想想</el-button>
                <el-button size="small" type="danger" :loading="submitting" @click="order_delete">确定取消</el-button>
            </div>
        </el-dialog>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import listPager from '@/mixins/listPager'
import { fmtMoney } from '@/utils/format'

/**
 * 用户端 - 待派单订单（原「未发货订单」）。
 *
 * 接口不变：GET / POST / DELETE /api/user/unsend。
 * 取消订单的副作用（m_sale_v 与 orderway.count 回退）由数据库触发器负责，
 * 前端只负责提示，不做任何本地推算。
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
            filter: { keyword: '' },
            page: 1,
            pageSize: 10,
            dialog_chnage: false,
            dialog_delete: false,
            form_change: {
                order_id: '',
                cons_addre: '',
                cons_name: ''
            },
            delete_id: ''
        }
    },
    computed: {
        filterFields() {
            return ['shop_name', 'cons_name', 'cons_addre', 'orderway', 'create_time']
        }
    },
    methods: {
        money(v) {
            return fmtMoney(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/user/unsend')
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
        },
        showdia_ch(row) {
            this.form_change.order_id = row.order_id
            this.form_change.cons_name = row.cons_name
            this.form_change.cons_addre = row.cons_addre
            this.dialog_chnage = true
        },
        change() {
            if (!this.form_change.cons_name || !this.form_change.cons_addre) {
                this.$message({ message: '姓名与地址不能为空', type: 'warning' })
                return
            }
            this.submitting = true
            this.$axios
                .post('/api/user/unsend', this.form_change)
                .then((res) => {
                    if (res.data.status == 200) {
                        this.$message({ message: res.data.msg || '修改成功', type: 'success' })
                        this.dialog_chnage = false
                        this.getdata()
                    } else {
                        this.$message({ message: res.data.msg || '修改失败', type: 'error' })
                    }
                })
                .catch(() => this.$message({ message: '网络故障，修改失败', type: 'error' }))
                .finally(() => {
                    this.submitting = false
                })
        },
        showdia_dl(row) {
            this.delete_id = row.order_id
            this.dialog_delete = true
        },
        order_delete() {
            this.submitting = true
            this.$axios
                .delete('/api/user/unsend', { data: { delete_id: this.delete_id } })
                .then((res) => {
                    if (res.data.status == 200) {
                        this.$message({ message: res.data.msg || '删除成功', type: 'success' })
                        this.dialog_delete = false
                        this.getdata()
                    } else {
                        this.$message({ message: res.data.msg || '取消失败', type: 'error' })
                    }
                })
                .catch(() => this.$message({ message: '网络故障，取消失败', type: 'error' }))
                .finally(() => {
                    this.submitting = false
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

.table-ft {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 14px;
    flex-wrap: wrap;
}

.confirm-body {
    line-height: 1.9;
    color: var(--csu-text-sub);
}

.confirm-body b {
    color: var(--csu-text);
    margin: 0 3px;
}
</style>
