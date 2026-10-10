<template>
    <div class="login-page">
        <!-- 左侧品牌区：把项目的三条技术主线讲清楚，替代原来单一的深蓝底 -->
        <div class="login-visual">
            <div class="login-visual__inner">
                <div class="brand">
                    <span class="brand__mark">餐</span>
                    <div class="brand__text">
                        <b>校园快餐订餐管理系统</b>
                        <em>Campus Fast-Food Ordering</em>
                    </div>
                </div>

                <h1 class="visual-title">一餐一饭，<br>数据看得见。</h1>
                <p class="visual-desc">
                    从下单、派发到送达，每一步都落在数据库里：视图聚合状态、触发器维护销量与订餐方式计数。
                    管理端数据看板把这条链路实时算出来。
                </p>

                <ul class="visual-points">
                    <li><i class="el-icon-data-analysis"></i> 8 个统计面板 · 订单与营业额双轴走势</li>
                    <li><i class="el-icon-connection"></i> 视图 sending_order / sended_order 驱动状态流转</li>
                    <li><i class="el-icon-magic-stick"></i> 6 个触发器维护 orderway 与店铺销量</li>
                    <li><i class="el-icon-document"></i> MySQL 8 · Spring Boot 3 · Vue 2 + ECharts 5</li>
                </ul>
            </div>
        </div>

        <!-- 右侧登录卡片 -->
        <div class="login-panel">
            <div class="login-card">
                <h2 class="login-card__title">登录</h2>
                <p class="login-card__sub">请使用手机号与密码登录；管理员将进入后台数据看板</p>

                <el-form label-width="0" class="login_form" :model="login_form" :rules="login_rules"
                    ref="login_form">
                    <el-form-item prop="userortel">
                        <el-input v-model="login_form.userortel" spellcheck="false" placeholder="手机号"
                            prefix-icon="el-icon-mobile-phone" size="medium">
                        </el-input>
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input v-model="login_form.password" show-password spellcheck="false" placeholder="密码"
                            prefix-icon="el-icon-lock" size="medium" @keyup.enter.native="llogin()">
                        </el-input>
                    </el-form-item>

                    <el-form-item class="btns">
                        <el-button type="primary" class="login-btn" :loading="logging" @click="llogin()">
                            登 录
                        </el-button>
                    </el-form-item>
                </el-form>

                <div class="login-tip">
                    <p>演示账号（点击任一行可直接填入表单）</p>
                    <div class="login-accounts">
                        <button type="button" class="account" @click="fillAccount('manage')">
                            <em>管理端</em>
                            <span class="account__cred">18682412818 / Aa.460734909</span>
                            <i class="el-icon-right"></i>
                        </button>
                        <button type="button" class="account" @click="fillAccount('user')">
                            <em>用户端</em>
                            <span class="account__cred">18873190398 / aA.460734909</span>
                            <i class="el-icon-right"></i>
                        </button>
                    </div>
                    <p class="login-tip__note">
                        登录按<b>手机号</b>匹配（后端 SQL：<code>where telephone = ? and password = ?</code>），
                        数据库里的 username 字段不参与登录，因此不能填 CMX / YCX。
                    </p>
                </div>

                <p class="login-note csu-hint">
                    注册与找回密码依赖短信验证码接口，该路由未实现（返回 404），
                    因此本版未提供入口，避免进入无法完成的流程。
                </p>
            </div>
        </div>
    </div>
</template>

<script>
/**
 * 登录页。
 *
 * 相对原实现的说明：
 *   1. 布局改为「左侧品牌说明 + 右侧登录卡片」，配色统一到全站设计 token
 *      （原页面是纯深蓝底 + 居中白卡）；
 *   2. 登录逻辑与接口完全不变（POST /api/user/login，成功写 localStorage.token，
 *      role=0 进 /user，否则进 /manage）；
 *   3. 去掉了「注册」「忘记密码」入口：这两个页面依赖 /api/user/register/send_sms、
 *      /api/user/register/test、/api/user/findback，这些路由未实现
 *      （请求会 404），页面点进去也无法完成注册，属于死流程。
 *      删除入口而不是伪造一个假成功提示，避免误导使用者。
 *      相关接口状态见 README 的「接口一览」。
 */
export default {
    name: 'MyLogin',
    data() {
        var checkMobile = (rule, value, cb) => {
            const regMobile = /^(0|86|17951)?(13[0-9]|15[012356789]|17[678]|18[0-9]|14[57])[0-9]{8}$/
            if (regMobile.test(value)) {
                return cb()
            }
            cb(new Error('手机号码格式不正确'))
        }
        return {
            logging: false,
            login_form: {
                userortel: '',
                password: ''
            },
            login_rules: {
                userortel: [
                    { required: true, message: '请输入手机号', trigger: 'blur' },
                    { validator: checkMobile, trigger: 'blur' }
                ],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
            }
        }
    },
    methods: {
        /**
         * 一键填入演示账号。
         *
         * 说明：后端登录按 telephone 精确匹配（UserMapper.xml 的
         * `where telephone = ? and password = ?`），username 字段不参与登录，
         * 所以这里填的是手机号而不是 CMX / YCX。
         */
        fillAccount(role) {
            const accounts = {
                manage: { userortel: '18682412818', password: 'Aa.460734909' },
                user: { userortel: '18873190398', password: 'aA.460734909' }
            }
            const acc = accounts[role]
            if (!acc) return
            this.login_form.userortel = acc.userortel
            this.login_form.password = acc.password
            this.$nextTick(() => {
                if (this.$refs.login_form) this.$refs.login_form.clearValidate()
            })
            this.$message({ message: role === 'manage' ? '已填入管理端账号' : '已填入用户端账号', type: 'success' })
        },
        llogin() {
            this.$refs.login_form.validate((valid) => {
                if (!valid) return
                this.login()
            })
        },
        login() {
            this.logging = true
            this.$axios
                .post('/api/user/login', this.login_form)
                .then((res) => {
                    if (res.data.code != 200) {
                        this.$message({ message: res.data.msg, type: 'error' })
                        return
                    }
                    this.$message({ message: '登录成功', type: 'success' })
                    window.localStorage.setItem('token', res.data.token)
                    if (res.data.role == 0) this.$router.push('/user')
                    else this.$router.push('/manage')
                })
                .catch(() => {
                    this.$message({ message: '网络故障，请确认后端已启动（127.0.0.1:5000）', type: 'error' })
                })
                .finally(() => {
                    this.logging = false
                })
        }
    }
}
</script>

<style scoped>
.login-page {
    display: flex;
    height: 100%;
    background: var(--csu-bg);
}

/* ---------------- 左侧品牌区 ---------------- */
.login-visual {
    flex: 1 1 auto;
    min-width: 0;
    background: var(--csu-login-gradient);
    position: relative;
    overflow: hidden;
    display: flex;
    align-items: center;
}

/* 暖色光斑：让深色块不死板，同时呼应主色 */
.login-visual::after {
    content: '';
    position: absolute;
    right: -120px;
    bottom: -140px;
    width: 420px;
    height: 420px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(232, 98, 44, .38) 0%, rgba(232, 98, 44, 0) 68%);
}

.login-visual__inner {
    position: relative;
    z-index: 2;
    padding: 0 clamp(36px, 6vw, 88px);
    max-width: 640px;
}

.brand {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 46px;
}

.brand__mark {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    background: linear-gradient(135deg, var(--csu-primary) 0%, var(--csu-primary-gradient-to) 100%);
    color: var(--csu-text-invert);
    font-size: 19px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 14px rgba(232, 98, 44, .4);
}

.brand__text {
    display: flex;
    flex-direction: column;
    line-height: 1.35;
}

.brand__text b {
    color: var(--csu-text-invert);
    font-size: 15px;
    letter-spacing: .3px;
}

.brand__text em {
    font-style: normal;
    font-size: 11px;
    color: var(--csu-text-on-dark-dim);
    letter-spacing: 1px;
}

.visual-title {
    margin: 0 0 18px;
    font-size: clamp(26px, 3vw, 38px);
    line-height: 1.35;
    color: var(--csu-text-invert);
    font-weight: 700;
    letter-spacing: 1px;
}

.visual-title::after {
    content: '';
    display: block;
    width: 54px;
    height: 3px;
    border-radius: 2px;
    background: var(--csu-primary);
    margin-top: 18px;
}

.visual-desc {
    margin: 0 0 30px;
    font-size: 13.5px;
    line-height: 2;
    color: var(--csu-text-on-dark-body);
    max-width: 480px;
}

.visual-points {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.visual-points li {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    color: var(--csu-text-on-dark);
}

.visual-points i {
    flex: none;
    width: 26px;
    height: 26px;
    border-radius: 7px;
    background: rgba(232, 98, 44, .16);
    color: var(--csu-primary-light);
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* ---------------- 右侧登录卡片 ---------------- */
.login-panel {
    flex: 0 0 480px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
}

.login-card {
    width: 100%;
    max-width: 372px;
}

.login-card__title {
    margin: 0 0 6px;
    font-size: 24px;
    font-weight: 700;
    color: var(--csu-text);
}

.login-card__sub {
    margin: 0 0 26px;
    font-size: 12.5px;
    color: var(--csu-text-weak);
    line-height: 1.7;
}

.login_form .el-form-item {
    margin-bottom: 20px;
}

.login-btn {
    width: 100%;
    letter-spacing: 4px;
}

.login-tip {
    margin-top: 26px;
    padding: 14px 16px;
    background: var(--csu-panel);
    border: 1px solid var(--csu-border);
    border-radius: var(--csu-radius);
}

.login-tip>p {
    margin: 0 0 10px;
    font-size: 12px;
    color: var(--csu-text-weak);
}

.login-accounts {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.account {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 7px 9px;
    border: 1px solid var(--csu-border-light);
    border-radius: var(--csu-radius-sm);
    background: var(--csu-panel-alt);
    font-size: 12.5px;
    text-align: left;
    cursor: pointer;
    transition: border-color .15s ease, background .15s ease;
}

.account:hover {
    border-color: var(--csu-border-primary);
    background: var(--csu-primary-bg);
}

.account em {
    flex: none;
    font-style: normal;
    padding: 1px 8px;
    border-radius: 10px;
    color: var(--csu-primary-text);
    background: var(--csu-topbar-bg);
    border: 1px solid var(--csu-border-primary);
    font-size: 11.5px;
}

.account__cred {
    flex: 1 1 auto;
    font-family: var(--csu-font-mono);
    color: var(--csu-text-sub);
    font-weight: 600;
    letter-spacing: .2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.account i {
    flex: none;
    color: var(--csu-text-weak);
    font-size: 12px;
}

.login-tip__note {
    margin: 10px 0 0 !important;
    padding-top: 9px;
    border-top: 1px dashed var(--csu-border);
    line-height: 1.75;
}

.login-tip__note b {
    color: var(--csu-text-sub);
}

.login-tip__note code {
    font-family: var(--csu-font-mono);
    font-size: 11px;
    padding: 1px 4px;
    border-radius: 3px;
    background: var(--csu-panel-alt);
    color: var(--csu-text-sub);
}

.login-note {
    margin: 16px 0 0;
    line-height: 1.8;
}

@media (max-width: 1080px) {
    .login-visual {
        display: none;
    }

    .login-panel {
        flex: 1 1 auto;
    }
}
</style>
