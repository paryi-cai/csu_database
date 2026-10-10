<template>
    <app-shell ref="shell" brand="校园快餐订餐管理系统" crumb-root="后台管理" :groups="menuGroups" :active="active"
        :crumbs="crumbs" :user-name="userName" role-text="超级管理员" @select="onSelect" @refresh="onRefresh"
        @logout="onLogout">
        <template slot="actions">
            <el-tag size="mini" effect="plain" class="round-tag">数据可视化演示版</el-tag>
        </template>

        <!-- 数据看板 -->
        <div v-if="visited.stats" v-show="active === 'stats'">
            <stats-dashboard ref="dashboard" :visible="active === 'stats' && statsVisible"></stats-dashboard>
        </div>

        <!-- 点单 -->
        <div v-if="visited['order-unsend']" v-show="active === 'order-unsend'">
            <order-unsend ref="orderUnsend"></order-unsend>
        </div>
        <div v-if="visited['order-sending']" v-show="active === 'order-sending'">
            <order-sending ref="orderSending"></order-sending>
        </div>
        <div v-if="visited['order-sended']" v-show="active === 'order-sended'">
            <order-sended ref="orderSended"></order-sended>
        </div>

        <!-- 配送 -->
        <div v-if="visited['wuliu-unended']" v-show="active === 'wuliu-unended'">
            <wuliu-unended ref="wuliuUnended"></wuliu-unended>
        </div>
        <div v-if="visited['wuliu-ended']" v-show="active === 'wuliu-ended'">
            <wuliu-ended ref="wuliuEnded"></wuliu-ended>
        </div>

        <!-- 门店 -->
        <div v-if="visited.shop" v-show="active === 'shop'">
            <manage-shop ref="manageShop"></manage-shop>
        </div>
        <div v-if="visited.server" v-show="active === 'server'">
            <manage-server ref="manageServer"></manage-server>
        </div>
        <div v-if="visited.dispatcher" v-show="active === 'dispatcher'">
            <manage-dispatcher ref="manageDispatcher"></manage-dispatcher>
        </div>
    </app-shell>
</template>

<script>
import AppShell from '@/components/common/AppShell.vue'

import StatsDashboard from '@/components/ManageStats/StatsDashboard.vue'
import ManageShop from '@/components/ManageShop.vue'
import ManageServer from '@/components/ManageServer.vue'
import ManageDispatcher from '@/components/ManageDispatcher.vue'
import WuliuEnded from '@/components/ManageWuliu/WuliuEnded.vue'
import WuliuUnended from '@/components/ManageWuliu/WuliuUnended.vue'
import OrderSended from '@/components/ManageOrder/BeSended.vue'
import OrderSending from '@/components/ManageOrder/BeSending.vue'
import OrderUnsend from '@/components/ManageOrder/UnSend.vue'

/**
 * 管理端主界面。
 *
 * 设计要点：
 *  1. 布局使用 AppShell（顶栏 + 侧栏 + 内容区），两个端共用同一套视觉规范；
 *  2. 菜单按业务划分为「总览 / 点单 / 配送 / 门店」四组，「数据看板」为默认页；
 *     文案统一为「待派单 / 配送中 / 已完成」——checked 本就是派发状态；
 *  3. 按需挂载：用 visited 记录"点开过才挂载"，挂载后保持存活，切换页面不重复请求。
 */
export default {
    components: {
        AppShell,
        StatsDashboard,
        ManageShop: ManageShop,
        ManageServer: ManageServer,
        ManageDispatcher: ManageDispatcher,
        WuliuEnded: WuliuEnded,
        WuliuUnended: WuliuUnended,
        OrderSended: OrderSended,
        OrderSending: OrderSending,
        OrderUnsend: OrderUnsend
    },
    data() {
        return {
            active: 'stats',
            /** 已挂载过的面板，切回来不再重新创建 */
            visited: { stats: true },
            /** 用于在看板重新可见时触发一次 ECharts resize */
            statsVisible: true,
            /** 顶栏显示名：优先用 user_msg 里的账号名，接口失败时保持默认 */
            userName: 'CMX',
            /** 菜单 index -> 面板 ref，供顶栏「刷新」转发到当前页面的 getdata() */
            refMap: {
                'order-unsend': 'orderUnsend',
                'order-sending': 'orderSending',
                'order-sended': 'orderSended',
                'wuliu-unended': 'wuliuUnended',
                'wuliu-ended': 'wuliuEnded',
                shop: 'manageShop',
                server: 'manageServer',
                dispatcher: 'manageDispatcher'
            },
            menuGroups: [
                {
                    title: '总览',
                    items: [{ index: 'stats', label: '数据看板', icon: 'el-icon-data-analysis' }]
                },
                {
                    title: '点单',
                    items: [
                        { index: 'order-unsend', label: '待派单', icon: 'el-icon-time' },
                        { index: 'order-sending', label: '配送中', icon: 'el-icon-bicycle' },
                        { index: 'order-sended', label: '已完成', icon: 'el-icon-circle-check' }
                    ]
                },
                {
                    title: '配送',
                    items: [
                        { index: 'wuliu-unended', label: '在途配送', icon: 'el-icon-truck' },
                        { index: 'wuliu-ended', label: '历史配送', icon: 'el-icon-finished' }
                    ]
                },
                {
                    title: '门店',
                    items: [
                        { index: 'shop', label: '店铺管理', icon: 'el-icon-s-shop' },
                        { index: 'server', label: '服务员管理', icon: 'el-icon-s-custom' },
                        { index: 'dispatcher', label: '配送员管理', icon: 'el-icon-s-check' }
                    ]
                }
            ],
            /** 菜单 index -> 面包屑文案 */
            titleMap: {
                stats: '数据看板',
                'order-unsend': '待派单',
                'order-sending': '配送中',
                'order-sended': '已完成',
                'wuliu-unended': '在途配送',
                'wuliu-ended': '历史配送',
                shop: '店铺管理',
                server: '服务员管理',
                dispatcher: '配送员管理'
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
        /** 顶栏显示真实账号名；接口失败时保持默认，不打断使用 */
        loadUserName() {
            this.$axios
                .get('/api/user/usermsg')
                .then((res) => {
                    if (res.data.status == 200 && res.data.data) {
                        this.userName = res.data.data.user_name || res.data.data.real_name || 'CMX'
                    }
                })
                .catch(() => {
                    /* 顶栏称呼属于锦上添花，失败静默 */
                })
        },
        onSelect(index) {
            this.active = index
            this.$set(this.visited, index, true)

            if (index === 'stats') {
                // v-show 隐藏期间容器尺寸为 0，ECharts 需要一次 resize 才能正确铺满
                this.statsVisible = false
                this.$nextTick(() => {
                    this.statsVisible = true
                })
            }
        },
        onRefresh() {
            if (this.active === 'stats') {
                const dashboard = this.$refs.dashboard
                if (dashboard && typeof dashboard.refresh === 'function') {
                    dashboard.refresh()
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
