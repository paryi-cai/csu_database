<template>
    <div class="chart-card csu-card csu-card--hover">
        <div class="chart-card__hd">
            <div class="chart-card__title-wrap">
                <div class="chart-card__title">
                    <span class="chart-card__mark" :style="{ background: accent }"></span>
                    {{ title }}
                </div>
                <div v-if="subtitle" class="chart-card__subtitle">{{ subtitle }}</div>
            </div>
            <div class="chart-card__tools">
                <slot name="tools"></slot>
            </div>
        </div>

        <div class="chart-card__bd" :style="{ height: height }">
            <!-- 图表常驻 DOM：ECharts 容器若被 v-if 反复销毁重建，resize/dispose 时序会变复杂。
                 加载与空态用绝对定位的覆盖层盖在上面（overlay 不拦截鼠标事件除外层按钮）。 -->
            <slot></slot>

            <!-- 空数据兜底：先于加载态判断，避免出现"一张空坐标系" -->
            <div v-if="!loading && isEmpty" class="overlay overlay--empty">
                <i class="el-icon-data-analysis"></i>
                <p>{{ emptyText }}</p>
                <el-button v-if="showRetry" type="text" size="mini" @click="$emit('retry')">
                    重新加载
                </el-button>
            </div>

            <!-- 骨架屏：请求中 -->
            <div v-else-if="loading" class="overlay overlay--skeleton">
                <div class="skeleton-bar" v-for="n in 6" :key="n" :style="skeletonStyle(n)"></div>
            </div>
        </div>

        <div v-if="$slots.footer" class="chart-card__ft">
            <slot name="footer"></slot>
        </div>
    </div>
</template>

<script>
/**
 * 图表卡片外壳：把「标题 + 副标题(口径说明) + 右上工具槽 + loading + 空态 + 图表」这套
 * 每张图都要写一遍的结构收敛成一个组件，保证 8 个面板视觉与交互完全一致。
 *
 * 用法：图表放进默认插槽（slot），卡片负责外层容器与状态覆盖层。
 * 空态判定由父组件给出（isEmpty），因为"什么算空"因图而异：
 * 折线图看数组长度、饼图看合计是否为 0。
 */
export default {
    name: 'ChartCard',
    props: {
        title: { type: String, required: true },
        /** 副标题用于写清统计口径，例如"按 oorder.create_time 按天聚合" */
        subtitle: { type: String, default: '' },
        /** 卡片左上角色条颜色，用来在网格里区分不同主题的图 */
        accent: { type: String, default: '#E8622C' },
        height: { type: String, default: '300px' },
        loading: { type: Boolean, default: false },
        isEmpty: { type: Boolean, default: false },
        emptyText: { type: String, default: '暂无数据' },
        /** 空态里是否显示「重试」按钮（由调用方决定要不要给重试入口） */
        showRetry: { type: Boolean, default: true },
    },
    methods: {
        /** 骨架条高度错落排列，比等高条纹更像"正在成形的图表" */
        skeletonStyle(n) {
            const heights = ['46%', '68%', '34%', '82%', '58%', '72%']
            return { height: heights[(n - 1) % heights.length] }
        }
    }
}
</script>

<style scoped>
.chart-card {
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.chart-card__hd {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 16px 10px;
    flex: none;
}

.chart-card__title-wrap {
    min-width: 0;
}

.chart-card__title {
    display: flex;
    align-items: center;
    font-size: 14px;
    font-weight: 600;
    color: var(--csu-text);
    line-height: 20px;
}

.chart-card__mark {
    width: 3px;
    height: 14px;
    border-radius: 2px;
    margin-right: 8px;
    flex: none;
}

.chart-card__subtitle {
    margin-top: 4px;
    margin-left: 11px;
    font-size: 12px;
    color: var(--csu-text-weak);
    line-height: 16px;
}

.chart-card__tools {
    flex: none;
    display: flex;
    align-items: center;
    gap: 8px;
}

.chart-card__bd {
    position: relative;
    padding: 0 8px 6px;
    flex: 1 1 auto;
    min-height: 0;
}

.overlay {
    position: absolute;
    inset: 0;
    z-index: 3;
}

.overlay--empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--csu-text-weak);
    /* 覆盖层不吞掉鼠标事件，但内部按钮要可点 */
    pointer-events: none;
}

.overlay--empty .el-button {
    pointer-events: auto;
}

.overlay--empty i {
    font-size: 34px;
    color: var(--csu-icon-empty);
}

.overlay--empty p {
    margin: 10px 0 4px;
    font-size: 13px;
}

.overlay--skeleton {
    display: flex;
    align-items: flex-end;
    gap: 10px;
    padding: 16px 8px 12px;
    background: var(--csu-panel);
}

.skeleton-bar {
    flex: 1;
    border-radius: 3px 3px 0 0;
    background: linear-gradient(180deg, var(--csu-skeleton-from) 0%, var(--csu-skeleton-to) 100%);
    animation: csu-skeleton 1.1s ease-in-out infinite alternate;
}

.skeleton-bar:nth-child(2n) {
    animation-delay: .18s;
}

@keyframes csu-skeleton {
    from {
        opacity: .55;
    }

    to {
        opacity: 1;
    }
}

.chart-card__ft {
    flex: none;
    padding: 8px 16px 12px;
    border-top: 1px solid var(--csu-border-light);
}
</style>
