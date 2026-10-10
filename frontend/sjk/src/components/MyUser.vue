<template>
    <app-shell ref="shell" brand="校园快餐订餐管理系统" crumb-root="点餐" :groups="menuGroups" :active="active"
        :crumbs="crumbs" :user-name="userName" role-text="校园顾客" @select="onSelect" @refresh="onRefresh"
        @logout="onLogout">
        <template slot="actions">
            <el-tag size="mini" effect="plain" class="round-tag">
                购物车 {{ cartCount }} 件
            </el-tag>
        </template>

        <div v-if="visited.shop" v-show="active === 'shop'">
            <user-shop ref="shop" @cart-change="onCartChange"></user-shop>
        </div>

        <div v-if="visited['order-unsend']" v-show="active === 'order-unsend'">
            <user-unsend ref="orderUnsend"></user-unsend>
        </div>
        <div v-if="visited['order-sending']" v-show="active === 'order-sending'">
            <user-sending ref="orderSending"></user-sending>
        </div>
        <div v-if="visited['order-finished']" v-show="active === 'order-finished'">
            <user-finished ref="orderFinished"></user-finished>
        </div>

        <!--
            个人中心只保留「个人信息」一项：查看资料、修改资料、修改密码都在这一页完成
            （改密码是页内弹窗，走 PUT /api/user/usermsg/pwd，语义与既有
            POST /api/user/pwd_chg 完全一致）。原先独立的「修改密码」菜单项已移除，
            避免两处入口做同一件事。
        -->
        <div v-if="visited.msg" v-show="active === 'msg'">
            <indi-msg ref="indiMsg"></indi-msg>
        </div>
    </app-shell>
</template>

<script>
import AppShell from '@/components/common/AppShell.vue'

import UserShop from '@/components/UserShop.vue'
import UserUnsend from '@/components/UserOrder/UserUnsend.vue'
import UserSending from '@/components/UserOrder/UserSending.vue'
import UserFinished from '@/components/UserOrder/UserFinished.vue'
import IndiMsg from '@/components/UserMsg/IndiMsg.vue'

/**
 * 用户端主界面。
 *
 * 与管理端共用 AppShell，保证两端是同一套视觉规范（顶栏 + 左侧导航 + 内容区）。
 * 菜单为三组五项：点餐（逛店铺）、我的订单（待派单 / 配送中 / 已完成）、
 * 个人中心（个人信息）。原「未发货 / 已发货」文案与 checked 的真实语义不符，
 * 已统一为「待派单 / 配送中 / 已完成」。
 *
 * 与 MyManage.vue 相同，面板改为"点开过才挂载"，避免首次进入同时发多组请求。
 * 购物车状态放在本组件，通过 cart-change 事件从店铺页同步，顶部标签实时显示件数。
 */
export default {
    components: {
        AppShell,
        UserShop,
        UserUnsend,
        UserSending,
        UserFinished,
        IndiMsg
    },
    data() {
        return {
            active: 'shop',
            visited: { shop: true },
            userName: '同学',
            cartCount: 0,
            /** 菜单 index -> 面板 ref，供顶栏「刷新」转发到当前页面的 getdata() */
            refMap: {
                'order-unsend': 'orderUnsend',
                'order-sending': 'orderSending',
                'order-finished': 'orderFinished',
                msg: 'indiMsg'
            },
            menuGroups: [
                {
                    title: '点餐',
                    items: [{ index: 'shop', label: '逛店铺', icon: 'el-icon-dish' }]
                },
                {
                    title: '我的订单',
                    items: [
                        { index: 'order-unsend', label: '待派单', icon: 'el-icon-time' },
                        { index: 'order-sending', label: '配送中', icon: 'el-icon-bicycle' },
                        { index: 'order-finished', label: '已完成', icon: 'el-icon-circle-check' }
                    ]
                },
                {
                    title: '个人中心',
                    items: [
                        { index: 'msg', label: '个人信息', icon: 'el-icon-user' }
                    ]
                }
            ],
            titleMap: {
                shop: '逛店铺',
                'order-unsend': '待派单',
                'order-sending': '配送中',
                'order-finished': '已完成',
                msg: '个人信息'
            }
        }
    },
    computed: {
        crumbs() {
            return [this.titleMap[this.active] || '']
        }
    },
    created() {
        this.loadUserName()
    },
    methods: {
        /** 顶栏显示真实姓名；接口失败时保持默认称呼，不打断用户 */
        loadUserName() {
            this.$axios
                .get('/api/user/usermsg')
                .then((res) => {
                    if (res.data.status == 200 && res.data.data) {
                        this.userName = res.data.data.real_name || res.data.data.user_name || '同学'
                    }
                })
                .catch(() => {
                    /* 顶栏称呼属于锦上添花，失败静默 */
                })
        },
        onSelect(index) {
            this.active = index
            this.$set(this.visited, index, true)
        },
        onCartChange(count) {
            this.cartCount = count
        },
        onRefresh() {
            if (this.active === 'shop') {
                const shop = this.$refs.shop
                if (shop && typeof shop.getdata === 'function') {
                    shop.getdata()
                    this.$message({ message: '店铺列表已刷新', type: 'success' })
                    return
                }
            }
            // 其余页面统一转发到面板自己的 getdata()，与面板内「刷新」按钮等价
            const panel = this.$refs[this.refMap[this.active]]
            if (panel && typeof panel.getdata === 'function') {
                panel.getdata()
                this.$message({ message: '已刷新当前页数据', type: 'success' })
                return
            }
            this.$message({ message: '当前页面已是最新数据', type: 'info' })
        },
        onLogout() {
            this.$confirm('确定要退出登录吗？', '退出登录', {
                confirmButtonText: '退出',
                cancelButtonText: '取消',
                type: 'warning'
            })
                .then(() => {
                    window.localStorage.removeItem('token')
                    this.$router.push('/login')
                })
                .catch(() => { })
        }
    }
}
</script>

<style scoped>
.round-tag {
    color: var(--csu-primary-text) !important;
    background: var(--csu-primary-bg) !important;
    border-color: var(--csu-border-primary) !important;
}
</style>
