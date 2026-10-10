<template>
    <page-panel title="服务员管理" subtitle="server 表 · 服务员与所属店铺（外键关联 fastfood_shop）" accent="#3E7CB1">
        <template slot="actions">
            <el-select v-model="filter.shop" size="small" clearable placeholder="按所属店铺筛选" class="filter-select">
                <el-option v-for="(item, index) in shop_range" :key="index" :label="item.shop_name"
                    :value="item.shop_name"></el-option>
            </el-select>
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索编号 / 姓名"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
            <el-button size="small" type="primary" icon="el-icon-plus" @click="showdia_add()">添加服务员</el-button>
        </template>

        <!-- 摘要条：与订单类页面同一套版式，避免只有几行数据时页面显得空 -->
        <div class="summary">
            <div class="summary__item">
                <em>服务员</em>
                <b class="csu-num">{{ total }}</b>
                <span>位</span>
            </div>
            <div class="summary__item">
                <em>覆盖店铺</em>
                <b class="csu-num">{{ shopCount }}</b>
                <span>家</span>
            </div>
            <div class="summary__item">
                <em>平均每店</em>
                <b class="csu-num">{{ avgPerShop }}</b>
                <span>人</span>
            </div>
            <div class="summary__item">
                <em>人员最多门店</em>
                <b class="summary__text">{{ topShop }}</b>
            </div>
        </div>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border
            empty-text="暂无服务员，点右上角「添加服务员」新增" max-height="calc(100vh - 340px)">
            <el-table-column prop="service_id" label="服务员编号" width="180" align="center" sortable></el-table-column>
            <el-table-column prop="service_name" label="姓名" min-width="160" align="center" sortable></el-table-column>
            <el-table-column prop="fastfood_shop_name" label="所属店铺" min-width="220" align="center" sortable>
                <template slot-scope="scope">
                    <span class="shop-chip">{{ scope.row.fastfood_shop_name }}</span>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
                <template slot-scope="scope">
                    <el-button size="mini" type="danger" plain @click="showdia_dlt(scope.row)">解雇</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="csu-hint list-ft">
            共 {{ total }} 位服务员<span v-if="hasFilter">（已筛选）</span>
        </div>

        <el-dialog title="添加服务员" :visible.sync="dia_add" width="30%" :close-on-click-modal="false">
            <el-form ref="add_form" :model="add_form" label-width="120px" :rules="add_form_rules">
                <el-form-item label="服务员编号：" prop="service_id">
                    <el-input v-model="add_form.service_id" placeholder="例如 100012"></el-input>
                </el-form-item>
                <el-form-item label="服务员姓名：" prop="service_name">
                    <el-input v-model="add_form.service_name"></el-input>
                </el-form-item>
                <el-form-item label="所属店铺：" prop="fastfood_shop_name">
                    <el-select v-model="add_form.fastfood_shop_name" placeholder="请选择店铺">
                        <el-option v-for="(item, index) in shop_range" :key="index" :label="item.shop_name"
                            :value="item.shop_name"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="dia_add = false">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="addserver()">确定添加</el-button>
            </div>
        </el-dialog>

        <el-dialog title="解雇服务员" :visible.sync="dia_dlt" width="30%">
            <div class="confirm-body">
                确定解雇服务员
                <b>{{ want_delete_name }}</b>
                （编号 {{ want_delete }}）吗？
            </div>
            <div slot="footer">
                <el-button size="small" @click="dia_dlt = false">取消</el-button>
                <el-button size="small" type="danger" :loading="submitting" @click="deleteserver()">确定解雇</el-button>
            </div>
        </el-dialog>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import listPager from '@/mixins/listPager'

/**
 * 服务员管理。
 *
 * GET /api/manager/server 除 tabledata 外还返回 shop_range（店铺候选），
 * 这里把它复用为"按所属店铺筛选"的下拉选项——原实现只把它喂给新增表单，
 * 列表本身没有任何筛选手段。
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
            pageSize: 0,
            dia_add: false,
            dia_dlt: false,
            add_form: {
                service_id: '',
                service_name: '',
                fastfood_shop_name: ''
            },
            want_delete: '',
            want_delete_name: '',
            add_form_rules: {
                service_id: [{ required: true, message: '必填项', trigger: 'blur' }],
                service_name: [{ required: true, message: '必填项', trigger: 'blur' }],
                fastfood_shop_name: [{ required: true, message: '必填项', trigger: 'blur' }]
            },
            shop_range: []
        }
    },
    computed: {
        filterFields() {
            return ['service_id', 'service_name', 'fastfood_shop_name']
        },
        hasFilter() {
            return !!(this.keywordTrimmed || this.filter.shop)
        },
        /** 覆盖店铺数（按 rawList 去重；摘要反映"全量盘子"而非筛选结果） */
        shopCount() {
            const set = new Set()
            this.rawList.forEach((row) => row.fastfood_shop_name && set.add(row.fastfood_shop_name))
            return set.size
        },
        avgPerShop() {
            if (!this.shopCount || !this.rawList.length) return '0.0'
            return (this.rawList.length / this.shopCount).toFixed(1)
        },
        /** 人员最多的门店（并列时取字典序最小的店名，结果稳定） */
        topShop() {
            if (!this.rawList.length) return '—'
            const counts = {}
            this.rawList.forEach((row) => {
                const key = row.fastfood_shop_name || '未知'
                counts[key] = (counts[key] || 0) + 1
            })
            const entries = Object.entries(counts).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
            return entries[0][0] + ' · ' + entries[0][1] + ' 人'
        },
        /** 在 mixin 的关键字过滤之上，再叠加"所属店铺"这一维 */
        filtered() {
            const base = this.applyKeywordFilter()
            const shop = this.filter.shop
            if (!shop) return base
            return base.filter((row) => row.fastfood_shop_name === shop)
        }
    },
    methods: {
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/manager/server')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                        this.shop_range = res.data.shop_range || []
                    }
                })
                .catch(() => this.$message({ message: '网络故障，服务员列表加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        },
        showdia_add() {
            this.add_form = { service_id: '', service_name: '', fastfood_shop_name: '' }
            this.dia_add = true
            this.$nextTick(() => {
                if (this.$refs.add_form) this.$refs.add_form.clearValidate()
            })
        },
        addserver() {
            this.$refs.add_form.validate((valid) => {
                if (!valid) return
                this.submitting = true
                this.$axios
                    .post('/api/manager/server', this.add_form)
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
        showdia_dlt(row) {
            this.want_delete = row.service_id
            this.want_delete_name = row.service_name
            this.dia_dlt = true
        },
        deleteserver() {
            this.submitting = true
            this.$axios
                .delete('/api/manager/server', { data: { want_delete: this.want_delete } })
                .then((res) => {
                    if (res.data.status == 200) {
                        this.$message({ message: res.data.msg, type: 'success' })
                        this.dia_dlt = false
                        this.getdata()
                    } else {
                        this.$message({ message: res.data.msg, type: 'error' })
                    }
                })
                .catch(() => this.$message({ message: '网络故障，解雇失败', type: 'error' }))
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
    width: 190px;
}

.filter-select {
    width: 180px;
}

.list-ft {
    margin-top: 12px;
}

.shop-chip {
    display: inline-block;
    padding: 1px 9px;
    border-radius: 10px;
    font-size: 12px;
    color: var(--csu-primary-text);
    background: var(--csu-primary-bg);
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
