<template>
    <span class="csu-status" :style="{ color: tone.color, background: tone.bg }">
        <i class="csu-status__dot" :style="{ background: tone.dot }"></i>{{ text }}
    </span>
</template>

<script>
import { ORDER_STATUS, ORDER_STATUS_FALLBACK_TONE } from '@/utils/format'

/**
 * 订单 / 物流状态标签。
 *
 * 颜色与看板图表严格一致（待派单=琥珀、配送中=主色橙、已完成=绿），
 * 这样"看板里的颜色"和"列表里的颜色"是同一套语义，不需要二次记忆。
 * 不用 el-tag 是因为 Element 的 tag 圆角/底色偏重，在密集表格里视觉噪音大。
 *
 * 文案与配色都取自 {@code utils/format.js} 的 {@code ORDER_STATUS}：
 * 那份字典同时被后端状态语义、看板图表和本组件消费，避免出现第三份 0/1/2 文案拷贝。
 */
export default {
    name: 'StatusTag',
    props: {
        /** 状态码：0 待派单 / 1 配送中 / 2 已完成；也可直接传文案型状态 */
        status: { type: [Number, String], default: 0 },
        /** 覆盖默认文案映射，例如物流页把 0 叫"在途"、1 叫"已结束" */
        labels: { type: Object, default: null }
    },
    computed: {
        text() {
            const key = String(this.status)
            if (this.labels && this.labels[key]) return this.labels[key]
            const item = ORDER_STATUS[key]
            return item ? item.label : String(this.status)
        },
        tone() {
            const item = ORDER_STATUS[String(this.status)]
            return item ? item.tone : ORDER_STATUS_FALLBACK_TONE
        }
    }
}
</script>

<style scoped>
.csu-status {
    display: inline-flex;
    align-items: center;
    height: 22px;
    padding: 0 9px;
    border-radius: 11px;
    font-size: 12px;
    line-height: 1;
    white-space: nowrap;
}

.csu-status__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    margin-right: 5px;
    flex: none;
}
</style>
