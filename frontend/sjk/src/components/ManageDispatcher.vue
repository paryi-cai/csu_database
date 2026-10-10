<template>
    <page-panel title="配送员管理" subtitle="dispatcher 表 · 新增 / 解雇" accent="#2FA36B">
        <template slot="actions">
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索编号 / 姓名 / 电话"></el-input>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
            <el-button size="small" type="primary" icon="el-icon-plus" @click="showdia_add()">添加配送员</el-button>
        </template>

        <el-table v-loading="loading" :data="pageData" class="csu-table" border
            empty-text="暂无配送员，点右上角「添加配送员」新增" max-height="calc(100vh - 340px)">
            <el-table-column prop="dispatcher_id" label="配送员编号" width="200" align="center" sortable></el-table-column>
            <el-table-column prop="dispatcher_name" label="姓名" width="200" align="center" sortable></el-table-column>
            <el-table-column prop="dispatcher_phone" label="联系电话" min-width="200" align="center" sortable></el-table-column>
            <el-table-column label="操作" width="140" align="center">
                <template slot-scope="scope">
                    <el-button size="mini" type="danger" plain @click="showdia_dlt(scope.row)">解雇</el-button>
                </template>
            </el-table-column>
        </el-table>

        <div class="csu-hint list-ft">
            共 {{ total }} 位配送员<span v-if="keywordTrimmed">（已按「{{ keywordTrimmed }}」筛选）</span>
        </div>

        <el-dialog title="添加配送员" :visible.sync="dia_add" width="30%" :close-on-click-modal="false">
            <el-form ref="add_form" :model="add_form" label-width="120px" :rules="add_form_rules">
                <el-form-item label="配送员编号：" prop="dispatcher_id">
                    <el-input v-model="add_form.dispatcher_id" placeholder="例如 010102"></el-input>
                </el-form-item>
                <el-form-item label="配送员姓名：" prop="dispatcher_name">
                    <el-input v-model="add_form.dispatcher_name"></el-input>
                </el-form-item>
                <el-form-item label="联系电话：" prop="dispatcher_phone">
                    <el-input v-model="add_form.dispatcher_phone"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="dia_add = false">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="adddispatcher()">确定添加</el-button>
            </div>
        </el-dialog>

        <el-dialog title="解雇配送员" :visible.sync="dia_dlt" width="30%">
            <div class="confirm-body">
                确定解雇配送员
                <b>{{ want_delete_name }}</b>
                （编号 {{ want_delete }}）吗？解雇后其历史配送记录仍会保留。
            </div>
            <div slot="footer">
                <el-button size="small" @click="dia_dlt = false">取消</el-button>
                <el-button size="small" type="danger" :loading="submitting" @click="deletedispatcher()">确定解雇</el-button>
            </div>
        </el-dialog>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import listPager from '@/mixins/listPager'

/**
 * 配送员管理。
 *
 * 接口与字段完全沿用原实现（GET/POST/DELETE /api/manager/dispatcher），
 * 本批次只做呈现层：卡片化布局、loading、空状态、关键字筛选、操作确认。
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
            dia_dlt: false,
            add_form: {
                dispatcher_id: '',
                dispatcher_name: '',
                dispatcher_phone: ''
            },
            want_delete: '',
            want_delete_name: '',
            add_form_rules: {
                dispatcher_id: [{ required: true, message: '必填项', trigger: 'blur' }],
                dispatcher_name: [{ required: true, message: '必填项', trigger: 'blur' }],
                dispatcher_phone: [{ required: true, message: '必填项', trigger: 'blur' }]
            }
        }
    },
    computed: {
        filterFields() {
            return ['dispatcher_id', 'dispatcher_name', 'dispatcher_phone']
        }
    },
    methods: {
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/manager/dispatcher')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                    } else {
                        this.$message({ message: res.data.msg || '加载失败', type: 'error' })
                    }
                })
                .catch(() => this.$message({ message: '网络故障，配送员列表加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        },
        showdia_add() {
            this.add_form = { dispatcher_id: '', dispatcher_name: '', dispatcher_phone: '' }
            this.dia_add = true
            this.$nextTick(() => {
                if (this.$refs.add_form) this.$refs.add_form.clearValidate()
            })
        },
        adddispatcher() {
            this.$refs.add_form.validate((valid) => {
                if (!valid) return
                this.submitting = true
                this.$axios
                    .post('/api/manager/dispatcher', this.add_form)
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
            this.want_delete = row.dispatcher_id
            this.want_delete_name = row.dispatcher_name
            this.dia_dlt = true
        },
        deletedispatcher() {
            this.submitting = true
            this.$axios
                .delete('/api/manager/dispatcher', { data: { want_delete: this.want_delete } })
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
.filter-input {
    width: 220px;
}

.list-ft {
    margin-top: 12px;
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
