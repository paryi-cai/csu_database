<template>
    <div class="page-panel csu-card">
        <div class="page-panel__hd">
            <div class="page-panel__lead">
                <span class="page-panel__bar" :style="{ background: accent }"></span>
                <h2 class="page-panel__title">{{ title }}</h2>
                <span v-if="subtitle" class="page-panel__subtitle">{{ subtitle }}</span>
            </div>
            <div class="page-panel__actions">
                <slot name="actions"></slot>
            </div>
        </div>
        <div class="page-panel__bd">
            <slot></slot>
        </div>
    </div>
</template>

<script>
/**
 * 页面内容卡片：统一原先各子页面里那份"64px 居中方块标题 + 底部灰线"的 header。
 * 标题左对齐并加一条主色竖条，右侧留给筛选/操作按钮——这样列表页的
 * 「筛选 + 表格 + 分页」范式就有了统一的承载容器。
 */
export default {
    name: 'PagePanel',
    props: {
        title: { type: String, required: true },
        /** 一句话说明本页数据口径/来源 */
        subtitle: { type: String, default: '' },
        accent: { type: String, default: '#E8622C' },
    }
}
</script>

<style scoped>
.page-panel {
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.page-panel__hd {
    flex: none;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    padding: 13px 16px;
    border-bottom: 1px solid var(--csu-border-light);
}

.page-panel__lead {
    display: flex;
    align-items: center;
    min-width: 0;
}

.page-panel__bar {
    width: 3px;
    height: 15px;
    border-radius: 2px;
    margin-right: 9px;
    flex: none;
}

.page-panel__title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--csu-text);
    line-height: 22px;
    white-space: nowrap;
}

.page-panel__subtitle {
    margin-left: 12px;
    padding-left: 12px;
    border-left: 1px solid var(--csu-border);
    font-size: 12px;
    color: var(--csu-text-weak);
    line-height: 16px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.page-panel__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.page-panel__bd {
    flex: 1 1 auto;
    padding: 16px;
    min-width: 0;
}

</style>
