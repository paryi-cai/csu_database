<template>
    <page-panel title="店铺管理" subtitle="fastfood_shop 表 · 单价与销量（销量由触发器维护）" accent="#E8622C">
        <template slot="actions">
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索店铺名称"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
            <el-button size="small" type="primary" icon="el-icon-plus" @click="showdia_add()">添加店铺</el-button>
        </template>

        <!-- 摘要条：与订单类页面同一套版式；数据随搜索/筛选联动 -->
        <div class="summary">
            <div class="summary__item">
                <em>店铺</em>
                <b class="csu-num">{{ total }}</b>
                <span>家</span>
            </div>
            <div class="summary__item">
                <em>销量合计（m_sale_v）</em>
                <b class="csu-num">{{ saleTotal }}</b>
                <span>单</span>
            </div>
            <div class="summary__item">
                <em>营业额估算</em>
                <b class="csu-num">{{ money(turnoverTotal) }}</b>
                <span>元</span>
            </div>
            <div class="summary__item">
                <em>单价区间</em>
                <b class="summary__text">{{ priceRange }}</b>
            </div>
        </div>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border
            empty-text="暂无店铺，点右上角「添加店铺」新增" max-height="calc(100vh - 340px)">
            <el-table-column prop="shop_name" label="店铺名称" min-width="200" align="center" sortable></el-table-column>
            <el-table-column prop="price" label="产品单价（元）" width="160" align="right" sortable>
                <template slot-scope="scope">
                    <span class="csu-num">{{ money(scope.row.price) }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="sale" label="销量（单）" width="160" align="right" sortable>
                <template slot-scope="scope">
                    <span class="csu-num">{{ num(scope.row.sale) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="营业额估算（元）" width="180" align="right" sortable :sort-method="sortByTurnover">
                <template slot-scope="scope">
                    <span class="csu-num turnover">{{ money(scope.row.price * scope.row.sale) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="170" align="center">
                <template slot-scope="scope">
                    <el-button size="mini" type="warning" plain @click="showdia_chg(scope.row)">修改</el-button>
                    <el-button size="mini" type="danger" plain @click="showdia_dlt(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="csu-hint list-ft">
            共 {{ total }} 家店铺<span v-if="keywordTrimmed">（已按「{{ keywordTrimmed }}」筛选）</span>
            · 营业额估算 = 单价 × 销量，仅供参考，看板中的营业额来自 oorder 实时聚合
        </div>

        <el-dialog title="添加店铺" :visible.sync="dia_add" width="30%" :close-on-click-modal="false">
            <el-form ref="add_form" :model="add_form" label-width="100px" :rules="add_form_rules">
                <el-form-item label="店铺名称：" prop="shop_name">
                    <el-input v-model="add_form.shop_name"></el-input>
                </el-form-item>
                <el-form-item label="产品单价：" prop="price">
                    <el-input v-model="add_form.price" placeholder="整数，单位：元"></el-input>
                </el-form-item>
                <el-form-item label="销量：" prop="m_sale_v">
                    <el-input v-model="add_form.m_sale_v" placeholder="整数，单位：单"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="dia_add = false">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="addshop()">确定添加</el-button>
            </div>
        </el-dialog>

        <el-dialog title="修改店铺" :visible.sync="dia_chg" width="30%" :close-on-click-modal="false">
            <el-form ref="chg_form" :model="chg_form" label-width="100px">
                <el-form-item label="店铺名称：">
                    <span class="readonly-value">{{ chg_form.shop_name }}</span>
                </el-form-item>
                <el-form-item label="产品单价：">
                    <el-input v-model="chg_form.price"></el-input>
                </el-form-item>
                <el-form-item label="销量：">
                    <el-input v-model="chg_form.m_sale_v"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="dia_chg = false">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="changeshop()">保存修改</el-button>
            </div>
        </el-dialog>

        <el-dialog title="删除店铺" :visible.sync="dia_dlt" width="30%">
            <div class="confirm-body">
                确定删除店铺
                <b>{{ want_delete }}</b>
                吗？
                <span class="confirm-body__warn">
                    注意：数据库<b>不会</b>阻止删除仍被服务员引用的店铺（server 表上没有该外键），
                    删除后那些服务员会变成指向不存在店铺的孤儿记录。建议先解雇该店铺的服务员。
                </span>
            </div>
            <div slot="footer">
                <el-button size="small" @click="dia_dlt = false">取消</el-button>
                <el-button size="small" type="danger" :loading="submitting" @click="deleteshop()">确定删除</el-button>
            </div>
        </el-dialog>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import listPager from '@/mixins/listPager'
import { fmtMoney, fmtNum } from '@/utils/format'

/**
 * 店铺管理。
 *
 * 接口沿用原实现：GET / POST(action=add|change) / DELETE /api/manager/shop。
 * 新增的是呈现层：卡片布局、loading、空态、筛选、按钮提交态，
 * 以及「营业额估算」列（单价 × 销量）——让店铺页与看板的店铺业绩榜形成呼应。
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
            pageSize: 0,
            dia_add: false,
            dia_chg: false,
            dia_dlt: false,
            add_form: {
                shop_name: '',
                price: '',
                m_sale_v: '',
                action: 'add'
            },
            chg_form: {
                shop_name: '',
                price: '',
                m_sale_v: '',
                action: 'change'
            },
            want_delete: '',
            add_form_rules: {
                shop_name: [{ required: true, message: '必填项', trigger: 'blur' }],
                price: [{ required: true, message: '必填项', trigger: 'blur' }],
                m_sale_v: [{ required: true, message: '必填项', trigger: 'blur' }]
            }
        }
    },
    computed: {
        filterFields() {
            return ['shop_name']
        },
        /** 销量合计：与列表口径一致，随搜索结果联动 */
        saleTotal() {
            return this.filtered.reduce((sum, row) => sum + Number(row.sale || 0), 0)
        },
        /** 营业额估算合计 = Σ(单价 × 销量)，与表格最后一列的算法保持一致 */
        turnoverTotal() {
            return this.filtered.reduce(
                (sum, row) => sum + Number(row.price || 0) * Number(row.sale || 0), 0
            )
        },
        priceRange() {
            if (!this.filtered.length) return '—'
            const prices = this.filtered.map((row) => Number(row.price || 0))
            const min = Math.min(...prices)
            const max = Math.max(...prices)
            return min === max ? min + ' 元' : min + ' ~ ' + max + ' 元'
        }
    },
    methods: {
        /** 「营业额估算」列的排序口径：单价 × 销量（Element 默认按 prop 排序，本列无 prop） */
        sortByTurnover(a, b) {
            return Number(a.price || 0) * Number(a.sale || 0) - Number(b.price || 0) * Number(b.sale || 0)
        },
        num(v) {
            return fmtNum(v)
        },
        money(v) {
            return fmtMoney(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/manager/shop')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                    }
                })
                .catch(() => this.$message({ message: '网络故障，店铺列表加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        },
        showdia_add() {
            this.add_form = { shop_name: '', price: '', m_sale_v: '', action: 'add' }
            this.dia_add = true
            this.$nextTick(() => {
                if (this.$refs.add_form) this.$refs.add_form.clearValidate()
            })
        },
        addshop() {
            this.$refs.add_form.validate((valid) => {
                if (!valid) return
                this.submitting = true
                this.$axios
                    .post('/api/manager/shop', this.add_form)
                    .then((res) => {
                        if (res.data.status == 200) {
                            this.$message({ message: '添加成功', type: 'success' })
                            this.dia_add = false
                            this.getdata()
                        } else {
                            this.$message({ message: res.data.msg, type: 'error' })
                        }
                    })
                    .catch(() => this.$message({ message: '网络故障，添加失败', type: 'error' }))
                    .finally(() => {
                        this.submitting = false
                    })
            })
        },
        showdia_chg(row) {
            this.chg_form.shop_name = row.shop_name
            this.chg_form.price = row.price
            this.chg_form.m_sale_v = row.sale
            this.dia_chg = true
        },
        changeshop() {
            this.submitting = true
            this.$axios
                .post('/api/manager/shop', this.chg_form)
                .then((res) => {
                    if (res.data.status == 200) {
                        this.$message({ message: '修改成功', type: 'success' })
                        this.dia_chg = false
                        this.getdata()
                    } else {
                        this.$message({ message: res.data.msg, type: 'error' })
                    }
                })
                .catch(() => this.$message({ message: '网络故障，修改失败', type: 'error' }))
                .finally(() => {
                    this.submitting = false
                })
        },
        showdia_dlt(row) {
            this.want_delete = row.shop_name
            this.dia_dlt = true
        },
        deleteshop() {
            this.submitting = true
            this.$axios
                .delete('/api/manager/shop', { data: { want_delete: this.want_delete } })
                .then((res) => {
                    if (res.data.status == 200) {
                        this.$message({ message: res.data.msg, type: 'success' })
                        this.dia_dlt = false
                        this.getdata()
                    } else {
                        // 业务失败：例如店铺名不存在、参数不合法。
                        // 注意删除**不会**因"被服务员引用"而失败——生产库没有那个外键
                        // （详见 README「已知差异」第 12 条），那种情况会删除成功并留下孤儿行。
                        this.$message({ message: res.data.msg || '删除失败', type: 'error' })
                    }
                })
                .catch(() => this.$message({ message: '网络故障，删除失败', type: 'error' }))
                .finally(() => {
                    this.submitting = false
                })
        }
    }
}
</script>

<style scoped>
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

@media (max-width: 1200px) {
    .summary {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

.filter-input {
    width: 220px;
}

.list-ft {
    margin-top: 12px;
}

.turnover {
    color: var(--csu-text-sub);
}

.readonly-value {
    color: var(--csu-text);
    font-weight: 600;
}

.confirm-body {
    line-height: 1.9;
    color: var(--csu-text-sub);
}

.confirm-body__warn {
    display: block;
    margin-top: 10px;
    padding: 8px 10px;
    border-radius: var(--csu-radius-sm);
    background: var(--csu-primary-bg);
    border: 1px solid var(--csu-border-primary);
    color: var(--csu-primary-text);
    font-size: 12.5px;
    line-height: 1.7;
}

.confirm-body b {
    color: var(--csu-text);
    margin: 0 3px;
}
</style>
