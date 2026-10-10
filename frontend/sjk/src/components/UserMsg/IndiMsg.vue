<template>
    <page-panel title="个人信息" subtitle="user_msg 表 · 外键关联 user.id（联系电话是查询键，只读）" accent="#3E7CB1">
        <template slot="actions">
            <el-button v-if="!editing" size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
            <el-button v-if="!editing" size="small" type="primary" icon="el-icon-edit" @click="startEdit">
                修改资料
            </el-button>
            <template v-else>
                <el-button size="small" @click="cancelEdit">取消</el-button>
                <el-button size="small" type="primary" icon="el-icon-check" :loading="saving" @click="save">
                    保存修改
                </el-button>
            </template>
        </template>

        <div v-loading="loading" class="profile">
            <div class="profile__side">
                <div class="profile__avatar">
                    <span>{{ avatarText }}</span>
                </div>
                <div class="profile__name">{{ form.real_name || form.user_name || '未填写' }}</div>
                <div class="profile__sub">{{ form.user_name || '—' }}</div>
                <el-button class="profile__pwd-btn" size="mini" icon="el-icon-lock" @click="pwdVisible = true">
                    修改密码
                </el-button>
            </div>

            <!-- 查看态 -->
            <div v-if="!editing" class="profile__body">
                <div v-for="row in viewRows" :key="row.label" class="profile__row">
                    <em>{{ row.label }}</em>
                    <b :class="{ 'csu-num': row.mono }">{{ row.value }}</b>
                    <span v-if="row.tag" class="profile__tag">{{ row.tag }}</span>
                </div>
            </div>

            <!-- 编辑态：只有真实存在于 DTO 白名单里的 5 个字段是输入框，其余展示为只读 -->
            <div v-else class="profile__body">
                <el-form ref="form" :model="form" :rules="rules" label-width="96px" class="profile__form">
                    <el-form-item label="用户名：" prop="user_name">
                        <el-input v-model="form.user_name" placeholder="相当于昵称，不影响登录"></el-input>
                    </el-form-item>
                    <el-form-item label="真实姓名：" prop="real_name">
                        <el-input v-model="form.real_name"></el-input>
                    </el-form-item>
                    <el-form-item label="性别：" prop="sex">
                        <el-radio-group v-model="form.sex">
                            <el-radio label="男">男</el-radio>
                            <el-radio label="女">女</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="年龄：" prop="age">
                        <el-input-number v-model="form.age" :min="1" :max="120" controls-position="right"
                            style="width: 160px"></el-input-number>
                    </el-form-item>
                    <el-form-item label="邮箱：" prop="mail">
                        <el-input v-model="form.mail" placeholder="例如 320836@qq.com"></el-input>
                    </el-form-item>
                    <el-form-item label="联系电话：">
                        <el-input :value="form.phone" disabled></el-input>
                        <div class="csu-hint profile__hint">
                            该号码是 <code>GET /api/user/usermsg</code> 的查询键
                            （后端 SQL：<code>where phone = ?</code>），可改会导致记录失联，因此设为只读。
                        </div>
                    </el-form-item>
                </el-form>
            </div>
        </div>

        <div v-if="!loading && !hasData" class="profile-empty">
            <i class="el-icon-user"></i>
            <p>数据库中没有该账号的明细记录（user_msg 表按 user.id 关联）</p>
        </div>

        <!-- 修改密码：与独立页面同一套语义（原密码校验） -->
        <el-dialog title="修改密码" :visible.sync="pwdVisible" width="420px" :close-on-click-modal="false">
            <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="90px">
                <el-form-item label="原密码：" prop="old_pwd">
                    <el-input v-model="pwdForm.old_pwd" type="password" show-password></el-input>
                </el-form-item>
                <el-form-item label="新密码：" prop="new_pwd">
                    <el-input v-model="pwdForm.new_pwd" type="password" show-password></el-input>
                </el-form-item>
                <el-form-item label="确认密码：" prop="check_pwd">
                    <el-input v-model="pwdForm.check_pwd" type="password" show-password></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="pwdVisible = false">取消</el-button>
                <el-button size="small" type="primary" :loading="changingPwd" @click="changePwd">确定修改</el-button>
            </div>
        </el-dialog>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import { fmtText } from '@/utils/format'

/**
 * 用户端 - 个人信息（查看 / 修改资料 / 修改密码）。
 *
 * 接口：
 *   GET  /api/user/usermsg        查询（响应 data 为对象，Flask 契约）
 *   PUT  /api/user/usermsg/edit   修改 5 个字段
 *   PUT  /api/user/usermsg/pwd    改密码（语义与 POST /api/user/pwd_chg 一致）
 *
 * 「联系电话」为什么是只读：它是 GET /api/user/usermsg 的查询键
 * （后端 `select * from user_msg where phone = ?`），一旦允许修改，
 * 用户改完就再也查不到自己的记录。这一取舍在页面上直接写明，避免看起来像漏做了。
 */
export default {
    components: { PagePanel },
    created() {
        this.getdata()
    },
    data() {
        return {
            loading: false,
            saving: false,
            changingPwd: false,
            editing: false,
            hasData: false,
            pwdVisible: false,
            form: {
                user_name: '',
                real_name: '',
                sex: '',
                age: undefined,
                mail: '',
                phone: ''
            },
            rules: {
                user_name: [{ required: true, message: '请填写用户名', trigger: 'blur' }],
                real_name: [{ required: true, message: '请填写真实姓名', trigger: 'blur' }],
                sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
                age: [{ required: true, message: '请填写年龄', trigger: 'blur' }],
                mail: [
                    { required: true, message: '请填写邮箱', trigger: 'blur' },
                    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
                ]
            },
            pwdForm: { old_pwd: '', new_pwd: '', check_pwd: '' },
            pwdRules: {
                old_pwd: [{ required: true, message: '必填', trigger: 'blur' }],
                new_pwd: [
                    { required: true, message: '必填', trigger: 'blur' },
                    { min: 6, message: '长度不少于 6 位', trigger: 'blur' }
                ],
                check_pwd: [{ required: true, message: '必填', trigger: 'blur' }]
            }
        }
    },
    computed: {
        avatarText() {
            const name = this.form.real_name || this.form.user_name || ''
            return name ? name.slice(0, 1) : '客'
        },
        viewRows() {
            return [
                { label: '用户名', value: this.text(this.form.user_name) },
                { label: '真实姓名', value: this.text(this.form.real_name) },
                { label: '性别', value: this.text(this.form.sex) },
                { label: '年龄', value: this.text(this.form.age), mono: true },
                { label: '联系电话', value: this.text(this.form.phone), mono: true, tag: '只读' },
                { label: '邮箱', value: this.text(this.form.mail) }
            ]
        }
    },
    methods: {
        text(v) {
            return fmtText(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/user/usermsg')
                .then((res) => {
                    const data = res.data && res.data.data
                    if (res.data.status == 200 && data) {
                        this.form.user_name = data.user_name
                        this.form.real_name = data.real_name
                        this.form.sex = data.sex
                        this.form.age = data.age
                        this.form.mail = data.mail
                        this.form.phone = data.phone
                        this.hasData = true
                    } else {
                        this.hasData = false
                    }
                })
                .catch(() => this.$message({ message: '网络故障，个人信息加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        },
        startEdit() {
            this.editing = true
            this.$nextTick(() => {
                if (this.$refs.form) this.$refs.form.clearValidate()
            })
        },
        cancelEdit() {
            this.editing = false
            this.getdata()
        },
        save() {
            this.$refs.form.validate((valid) => {
                if (!valid) return
                this.saving = true
                // 只提交 DTO 白名单里的 5 个字段：多传字段会被 Jackson 拒绝（严格模式）
                const payload = {
                    user_name: this.form.user_name,
                    real_name: this.form.real_name,
                    sex: this.form.sex,
                    age: this.form.age,
                    mail: this.form.mail
                }
                this.$axios
                    .put('/api/user/usermsg/edit', payload)
                    .then((res) => {
                        if (res.data.status == 200) {
                            this.$message({ message: res.data.msg || '修改成功', type: 'success' })
                            this.editing = false
                            this.getdata()
                        } else {
                            this.$message({ message: res.data.msg || '修改失败', type: 'error' })
                        }
                    })
                    .catch(() => this.$message({ message: '网络故障，保存失败', type: 'error' }))
                    .finally(() => {
                        this.saving = false
                    })
            })
        },
        changePwd() {
            this.$refs.pwdForm.validate((valid) => {
                if (!valid) return
                if (this.pwdForm.new_pwd !== this.pwdForm.check_pwd) {
                    this.$message({ message: '新密码与确认密码不一致', type: 'error' })
                    return
                }
                this.changingPwd = true
                this.$axios
                    .put('/api/user/usermsg/pwd', {
                        old_pwd: this.pwdForm.old_pwd,
                        new_pwd: this.pwdForm.new_pwd
                    })
                    .then((res) => {
                        if (res.data.status == 200) {
                            this.$message({ message: res.data.msg || '修改成功', type: 'success' })
                            this.pwdVisible = false
                            this.pwdForm = { old_pwd: '', new_pwd: '', check_pwd: '' }
                        } else {
                            this.$message({ message: res.data.msg || '修改失败', type: 'error' })
                        }
                    })
                    .catch(() => this.$message({ message: '网络故障，修改失败', type: 'error' }))
                    .finally(() => {
                        this.changingPwd = false
                    })
            })
        }
    }
}
</script>

<style scoped>
.profile {
    display: flex;
    gap: 30px;
    align-items: flex-start;
    max-width: 900px;
    padding: 6px 0;
}

.profile__side {
    flex: none;
    width: 150px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
}

.profile__avatar {
    width: 76px;
    height: 76px;
    border-radius: 50%;
    background: var(--csu-primary-bg);
    border: 1px solid var(--csu-border-primary);
    display: flex;
    align-items: center;
    justify-content: center;
}

.profile__avatar span {
    font-size: 30px;
    font-weight: 700;
    color: var(--csu-primary-text);
}

.profile__name {
    font-size: 15px;
    font-weight: 600;
    color: var(--csu-text);
    text-align: center;
    word-break: break-all;
}

.profile__sub {
    font-size: 12px;
    color: var(--csu-text-weak);
    margin-top: -4px;
}

.profile__pwd-btn {
    margin-top: 6px;
}

.profile__body {
    flex: 1 1 auto;
    min-width: 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px 26px;
}

.profile__row {
    display: flex;
    align-items: baseline;
    gap: 10px;
    padding: 9px 0;
    border-bottom: 1px dashed var(--csu-border);
    min-width: 0;
}

.profile__row em {
    flex: none;
    width: 72px;
    font-style: normal;
    font-size: 12.5px;
    color: var(--csu-text-weak);
}

.profile__row b {
    font-size: 14px;
    font-weight: 600;
    color: var(--csu-text);
    word-break: break-all;
}

.profile__tag {
    flex: none;
    font-size: 11px;
    padding: 1px 7px;
    border-radius: 9px;
    color: var(--csu-text-weak);
    background: var(--csu-panel-alt);
}

.profile__form {
    grid-column: 1 / -1;
    max-width: 460px;
}

.profile__hint {
    line-height: 1.7;
    padding-top: 2px;
}

.profile__hint code {
    font-family: var(--csu-font-mono);
    font-size: 11px;
    padding: 1px 4px;
    border-radius: 3px;
    background: var(--csu-panel-alt);
}

.profile-empty {
    padding: 30px 0;
    text-align: center;
    color: var(--csu-text-weak);
}

.profile-empty i {
    font-size: 34px;
    color: var(--csu-icon-empty);
}

.profile-empty p {
    margin: 10px 0 0;
    font-size: 13px;
}

@media (max-width: 900px) {
    .profile {
        flex-direction: column;
    }

    .profile__side {
        width: 100%;
        flex-direction: row;
        justify-content: flex-start;
        gap: 14px;
    }

    .profile__body {
        grid-template-columns: minmax(0, 1fr);
        width: 100%;
    }
}
</style>
