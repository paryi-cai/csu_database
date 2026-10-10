<template>
    <div class="shell">
        <!-- ==================== 顶部条：品牌 + 面包屑 + 用户区 ==================== -->
        <header class="topbar">
            <div class="topbar__brand" :style="{ width: collapsed ? 'var(--csu-side-w-collapsed)' : 'var(--csu-side-w)' }">
                <span class="brand__mark">餐</span>
                <span class="brand__text" v-show="!collapsed">{{ brand }}</span>
            </div>

            <button class="topbar__collapse" :title="collapsed ? '展开导航' : '收起导航'" @click="toggleCollapse">
                <i :class="collapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
            </button>

            <div class="topbar__crumb">
                <span class="crumb__root">{{ crumbRoot }}</span>
                <!--
                    面包屑是一串「分隔符 + 文本」，无法用单个元素承载，只能用 <template v-for>。
                    注意：Vue 2 **不允许**把 key 放在 <template> 上（ESLint: vue/no-v-for-template-key
                    会直接判编译错误），key 只能放在内部的真实元素上。
                -->
                <template v-for="(c, i) in crumbs">
                    <i class="el-icon-arrow-right crumb__sep" :key="'sep-' + i"></i>
                    <span class="crumb__item" :key="'crumb-' + i"
                        :class="{ 'is-current': i === crumbs.length - 1 }">
                        {{ c }}
                    </span>
                </template>
            </div>

            <div class="topbar__right">
                <!-- 页面级工具（刷新、切换等）由使用方塞进这里 -->
                <slot name="actions"></slot>

                <el-tooltip content="刷新当前页数据" placement="bottom" :open-delay="400">
                    <button class="topbar__icon" @click="$emit('refresh')">
                        <i class="el-icon-refresh-right"></i>
                    </button>
                </el-tooltip>

                <div class="topbar__divider"></div>

                <el-dropdown trigger="click" @command="onCommand">
                    <div class="user">
                        <span class="user__avatar">{{ avatarText }}</span>
                        <span class="user__meta">
                            <b class="user__name">{{ userName }}</b>
                            <em class="user__role">{{ roleText }}</em>
                        </span>
                        <i class="el-icon-arrow-down user__caret"></i>
                    </div>
                    <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item command="refresh" icon="el-icon-refresh">刷新数据</el-dropdown-item>
                        <el-dropdown-item command="logout" icon="el-icon-switch-button" divided>退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </el-dropdown>
            </div>
        </header>

        <div class="shell__body">
            <!-- ==================== 左侧导航 ==================== -->
            <aside class="side" :style="{ width: collapsed ? 'var(--csu-side-w-collapsed)' : 'var(--csu-side-w)' }">
                <el-menu :default-active="String(active)" :collapse="collapsed" :collapse-transition="false"
                    class="side__menu" background-color="transparent" text-color="var(--csu-sidebar-text)"
                    active-text-color="#FFFFFF" @select="onSelect">
                    <!--
                        分组标题与菜单项被拍平到同一个数组（flatSideItems），因此这里只需一层 v-for：
                        既能把 key 放在真实元素上（Vue 2 不允许 <template v-for> 带 key），
                        也避免 el-menu 的直接子级里夹一层 <template>——Element 解析 el-menu-item 时
                        依赖子级结构，夹包装层属于隐性风险。
                    -->
                    <el-menu-item v-for="item in flatSideItems" :key="item.key" :index="item.index"
                        :disabled="item.disabled" :class="{ 'side__item--head': item.type === 'group', 'side__item--hr': item.type === 'sep' }">
                        <!-- 三个分支必须互斥且穷尽：分隔行没有任何内容，若落进 v-else
                             会渲染出 <i class="side__icon"> 与字面量 "undefined"（虽然被
                             height:1px + overflow:hidden 掩盖，但会给 el-menu-item 注册一个
                             tooltip 内容为 "undefined"）。 -->
                        <template v-if="item.type === 'group'">{{ item.label }}</template>
                        <template v-else-if="item.type === 'item'">
                            <i :class="item.icon" class="side__icon"></i>
                            <span slot="title" class="side__label">{{ item.label }}</span>
                            <el-tag v-if="item.badge" size="mini" effect="dark" class="side__badge">{{ item.badge }}</el-tag>
                        </template>
                    </el-menu-item>
                </el-menu>

                <div class="side__ft" v-show="!collapsed">
                    <span>{{ brand }}</span>
                    <em>v1.0</em>
                </div>
            </aside>

            <!-- ==================== 内容区 ==================== -->
            <main class="content">
                <div class="content__inner">
                    <slot></slot>
                </div>
            </main>
        </div>
    </div>
</template>

<script>
/**
 * 全站统一外壳：顶部条 + 左侧导航 + 内容区。
 *
 * 背景：原项目里 MyManage.vue 与 MyUser.vue 各自手写了一份
 *       `.header + .liner(15%) + .main(85%)` 的布局，两侧样式几乎重复但已经出现
 *       差异（管理员页顶部无作者信息、用户页有）。这里收敛成一个组件，
 *       两个端共用同一套视觉规范，同时天然修掉"内容区固定 648px 高、
 *       小屏溢出、大屏留白"的老问题（内容区改为 flex + 独立滚动）。
 *
 * 使用方只需给出 groups（菜单）与 crumbs（面包屑），其余交互在外壳内完成。
 */
export default {
    name: 'AppShell',
    props: {
        /** 品牌名（沿用项目原有文案，未做改写） */
        brand: { type: String, default: '校园快餐订餐管理系统' },
        crumbRoot: { type: String, default: '首页' },
        /** 菜单：{ title?: '分组名', items: [{ index, label, icon, badge? }] } */
        groups: {
            type: Array,
            default: () => []
        },
        /** 当前激活的菜单 index（与 el-menu 的 index 同类型，内部统一转字符串比较） */
        active: { type: [String, Number], default: '' },
        userName: { type: String, default: '管理员' },
        roleText: { type: String, default: '' },
        /** 当前页面包屑（不含根节点） */
        crumbs: { type: Array, default: () => [] }
    },
    data() {
        return {
            collapsed: false
        }
    },
    computed: {
        avatarText() {
            const name = (this.userName || '').trim()
            return name ? name.slice(0, 1).toUpperCase() : 'A'
        },
        /**
         * 把 groups 拍平成单层列表：[分组标题] + [该组菜单项] + [折叠态分隔线] ...
         *
         * 为什么要拍平：模板里若用「<template v-for> 里再套 v-for」，Vue 2 不允许
         * 把 key 放在 <template> 上（ESLint vue/no-v-for-template-key 直接判错），
         * 而把 key 放到子节点上又会让外层循环失去 key。拍平后每个节点都有稳定 key，
         * 模板只剩一层 v-for，也顺带消除了 el-menu 直接子级夹包装层的隐性风险。
         *
         * **不可点击的条目必须 disabled: true**：分组标题/分隔线也复用了 el-menu-item，
         * 而 el-menu 的 handleItemClick 是**无条件** emit('select', index) 的
         * （Element 2.15 menu.vue，只在 item.index === null 时才不设 activeIndex）。
         * 若不给 disabled，点击标题行会 emit 一个无效 index，上层把 active 置成该值后
         * 所有面板的 v-show 全部判假 → 内容区整片空白；
         * 同时 el-menu 内部 addItem 用 `items[index]` 注册，多个条目共用同一个 index
         * 还会互相覆盖。disabled 会让 menu-item 的 handleClick 直接 return，从根上避免。
         */
        flatSideItems() {
            const flat = []
            this.groups.forEach((group, gi) => {
                if (group.title) {
                    if (this.collapsed) {
                        // 收起态：不 push 分组标题条目（宽度只有 64px，标题放不下，只会白占
                        // 一行 padding ≈35px × 分组数），改用一条细分隔线维持视觉分组。
                        flat.push({
                            type: 'sep',
                            index: '__sep_' + gi,
                            disabled: true,
                            key: 'sep-' + gi
                        })
                    } else {
                        flat.push({
                            type: 'group',
                            // index 必须唯一且可预测：el-menu 内部按 index 做 Map 注册
                            index: '__group_' + gi,
                            disabled: true,
                            key: 'group-' + gi,
                            label: group.title
                        })
                    }
                }
                const items = group.items || []
                items.forEach((item) => {
                    flat.push({
                        type: 'item',
                        key: 'item-' + item.index,
                        index: String(item.index),
                        disabled: false,
                        label: item.label,
                        icon: item.icon,
                        badge: item.badge
                    })
                })
            })
            return flat
        }
    },
    mounted() {
        this.syncCollapsedByWidth()
        window.addEventListener('resize', this.syncCollapsedByWidth)
    },
    beforeDestroy() {
        window.removeEventListener('resize', this.syncCollapsedByWidth)
    },
    methods: {
        /** 窄屏自动收起侧栏（< 1200px），宽屏保持用户手动选择 */
        syncCollapsedByWidth() {
            const narrow = window.innerWidth < 1200
            if (narrow === this.collapsed) return
            this.collapsed = narrow
        },
        toggleCollapse() {
            this.collapsed = !this.collapsed
            this.$emit('collapse-change', this.collapsed)
        },
        onSelect(index) {
            this.$emit('select', index)
        },
        onCommand(command) {
            if (command === 'logout') {
                this.$emit('logout')
            } else {
                this.$emit('refresh')
            }
        }
    }
}
</script>

<style scoped>
.shell {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: var(--csu-bg);
}

/* ---------------- 顶部条 ---------------- */
.topbar {
    flex: none;
    height: var(--csu-topbar-h);
    display: flex;
    align-items: stretch;
    background: var(--csu-topbar-bg);
    border-bottom: 1px solid var(--csu-border);
    box-shadow: 0 1px 2px rgba(42, 59, 76, .04);
    z-index: 10;
}

.topbar__brand {
    display: flex;
    align-items: center;
    gap: 9px;
    padding-left: 16px;
    background: var(--csu-sidebar);
    flex: none;
    overflow: hidden;
    transition: width .2s ease;
}

.brand__mark {
    flex: none;
    width: 28px;
    height: 28px;
    border-radius: 8px;
    background: linear-gradient(135deg, var(--csu-primary) 0%, var(--csu-primary-gradient-to) 100%);
    color: var(--csu-text-invert);
    font-size: 15px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(232, 98, 44, .35);
}

.brand__text {
    color: var(--csu-text-invert);
    font-size: 14px;
    font-weight: 600;
    white-space: nowrap;
    letter-spacing: .3px;
}

.topbar__collapse {
    flex: none;
    width: 46px;
    border: 0;
    background: transparent;
    color: var(--csu-text-sub);
    font-size: 18px;
    cursor: pointer;
    transition: background .15s ease, color .15s ease;
}

.topbar__collapse:hover {
    background: var(--csu-panel-alt);
    color: var(--csu-primary);
}

.topbar__crumb {
    flex: 1 1 auto;
    display: flex;
    align-items: center;
    gap: 7px;
    padding-left: 6px;
    min-width: 0;
    overflow: hidden;
    font-size: 13px;
}

.crumb__root {
    color: var(--csu-text-weak);
}

.crumb__sep {
    font-size: 11px;
    color: var(--csu-sep-on-dark);
}

.crumb__item {
    color: var(--csu-text-sub);
    white-space: nowrap;
}

.crumb__item.is-current {
    color: var(--csu-text);
    font-weight: 600;
}

.topbar__right {
    flex: none;
    display: flex;
    align-items: center;
    gap: 8px;
    padding-right: 14px;
}

.topbar__icon {
    width: 30px;
    height: 30px;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: var(--csu-text-sub);
    font-size: 16px;
    cursor: pointer;
    transition: background .15s ease, color .15s ease;
}

.topbar__icon:hover {
    background: var(--csu-primary-bg);
    color: var(--csu-primary);
}

.topbar__divider {
    width: 1px;
    height: 20px;
    background: var(--csu-border);
}

.user {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 8px 4px 4px;
    border-radius: 18px;
    cursor: pointer;
    transition: background .15s ease;
}

.user:hover {
    background: var(--csu-panel-alt);
}

.user__avatar {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--csu-primary-bg);
    color: var(--csu-primary-text);
    font-size: 13px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--csu-border-primary);
}

.user__meta {
    display: flex;
    flex-direction: column;
    line-height: 1.25;
}

.user__name {
    font-size: 13px;
    color: var(--csu-text);
    font-weight: 600;
}

.user__role {
    font-size: 11px;
    color: var(--csu-text-weak);
    font-style: normal;
}

.user__caret {
    font-size: 12px;
    color: var(--csu-text-weak);
}

/* ---------------- 主体 ---------------- */
.shell__body {
    flex: 1 1 auto;
    display: flex;
    min-height: 0;
}

.side {
    flex: none;
    background: var(--csu-sidebar);
    display: flex;
    flex-direction: column;
    transition: width .2s ease;
    overflow: hidden;
}

.side__menu {
    flex: 1 1 auto;
    border-right: 0;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 10px 0 16px;
}

.side__menu:not(.el-menu--collapse) {
    width: 100%;
}

/*
 * 分组标题行与分隔行也复用 el-menu-item（见 flatSideItems 的说明），
 * 因此必须把菜单项的尺寸/交互样式显式压掉，否则它们会像一个"可点击但没有内容的菜单项"。
 */
.side__menu .el-menu-item.side__item--head {
    height: auto;
    line-height: 1.4;
    padding: 14px 18px 6px !important;
    font-size: 11px;
    font-weight: 400;
    letter-spacing: 1px;
    color: var(--csu-sidebar-title);
    background-color: transparent !important;
    cursor: default;
    user-select: none;
}

.side__menu .el-menu-item.side__item--head:hover {
    background-color: transparent !important;
    color: var(--csu-sidebar-title) !important;
}

/* 分组标题/分隔线复用了 el-menu-item 并置为 disabled，这里覆盖 Element 给
   disabled 项的 opacity: .25（那会让标题淡到几乎看不见）与 not-allowed 光标 */
.side__menu .el-menu-item.is-disabled.side__item--head,
.side__menu .el-menu-item.is-disabled.side__item--hr {
    opacity: 1 !important;
    cursor: default !important;
}

.side__menu .el-menu-item.side__item--hr {
    height: 1px;
    min-height: 1px;
    padding: 0 !important;
    margin: 8px 14px;
    background-color: rgba(255, 255, 255, .08) !important;
    border-left-color: transparent;
    cursor: default;
    overflow: hidden;
}

.side__menu .el-menu-item.side__item--hr:hover {
    background-color: rgba(255, 255, 255, .08) !important;
}

.side__badge {
    margin-left: 6px;
    transform: scale(.85);
    background: var(--csu-primary);
    border-color: var(--csu-primary);
}

.side__ft {
    flex: none;
    padding: 10px 18px 12px;
    border-top: 1px solid rgba(255, 255, 255, .07);
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 11px;
    color: var(--csu-sidebar-title);
    white-space: nowrap;
}

.side__ft em {
    font-style: normal;
    opacity: .7;
}

.content {
    flex: 1 1 auto;
    min-width: 0;
    overflow: auto;
}

.content__inner {
    padding: var(--csu-page-pad);
    min-height: 100%;
    box-sizing: border-box;
}
</style>

<!-- 侧边菜单的 Element 内部样式必须用非 scoped 才能命中 -->
<style>
.side__menu .el-menu-item {
    /*
     * 用 flex 做"图标 + 文字"的垂直居中，而不是调整 line-height：
     * Element 的图标是字体，不同图标的字形墨迹高度不一致（el-icon-bicycle 与
     * el-icon-data-analysis 的视觉重心不在同一水平线上），只靠行高会出现
     * "有的图标偏上、有的偏下"。flex 的 align-items: center 按**盒子几何中心**对齐，
     * 与文字基线解耦，9 个菜单项的图标因此落在同一条水平中线上。
     *
     * padding-left 必须 !important：Element 会给 el-menu-item 写行内
     * padding-left: 20px，行内样式只输给 !important 的作者样式。
     */
    display: flex;
    align-items: center;
    height: 44px;
    line-height: 1;
    font-size: 13.5px;
    padding-left: 18px !important;
    padding-right: 12px;
    border-left: 3px solid transparent;
    transition: background .15s ease, border-color .15s ease, color .15s ease;
}

/* 图标固定盒：宽度统一，保证所有菜单项文字左边界落在同一条竖线上 */
.side__menu .el-menu-item i.side__icon {
    flex: none;
    width: 18px;
    height: 18px;
    line-height: 18px;
    font-size: 16px;
    text-align: center;
    color: inherit;
}

.side__menu .el-menu-item .side__label {
    flex: 1 1 auto;
    min-width: 0;
    margin-left: 8px;
    /* 覆盖 Element 给 slot=title 的 inline padding-left: 20px（图标已由 margin 留白） */
    padding-left: 0 !important;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.side__menu .el-menu-item:hover {
    background-color: var(--csu-sidebar-hover) !important;
    color: var(--csu-text-invert) !important;
}

.side__menu .el-menu-item.is-active {
    background-color: rgba(232, 98, 44, .16) !important;
    color: var(--csu-text-invert) !important;
    border-left-color: var(--csu-primary);
    font-weight: 600;
}

.side__menu .el-menu-item.is-active i.side__icon {
    color: var(--csu-primary-light);
}

/* 折叠态：图标水平居中，文字隐藏 */
.side__menu.el-menu--collapse .el-menu-item {
    padding-left: 0 !important;
    justify-content: center;
}

.side__menu.el-menu--collapse .el-menu-item .side__label {
    display: none;
}
</style>
