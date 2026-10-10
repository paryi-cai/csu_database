<template>
    <div class="kpi csu-card csu-card--hover">
        <div class="kpi__mark" :style="{ background: accent }"></div>

        <div class="kpi__hd">
            <span class="kpi__label">{{ label }}</span>
            <i :class="icon" class="kpi__icon" :style="{ color: accent, background: iconBg }"></i>
        </div>

        <div class="kpi__value">
            <span class="csu-num csu-num--lg">{{ displayValue }}</span>
            <span v-if="unit" class="csu-unit">{{ unit }}</span>
        </div>

        <div class="kpi__ft">
            <span class="kpi__hint">{{ hint }}</span>
            <span v-if="tag" class="kpi__tag" :style="{ color: tagColor, background: tagBg }">
                {{ tag }}
            </span>
        </div>
    </div>
</template>

<script>
import { thousand, compact } from '@/charts/echarts'

/**
 * KPI 指标卡。
 *
 * 与原项目/sky 的做法刻意区分：不用"彩色渐变图标块 + 大数字"的模板，
 * 改成「左侧 4px 语义色条 + 右上小图标 + 等宽数字 + 底部口径说明」。
 * 数字用等宽字体（--csu-font-mono），保证 6 张卡在数据刷新时数字不会左右跳动。
 */
export default {
    name: 'KpiCards',
    props: {
        label: { type: String, required: true },
        value: { type: [Number, String], default: 0 },
        unit: { type: String, default: '' },
        icon: { type: String, default: 'el-icon-data-line' },
        accent: { type: String, default: '#E8622C' },
        /** 底部一句话口径说明 */
        hint: { type: String, default: '' },
        /** 右上角小标签文本（如「+12%」）；为空则不渲染 */
        tag: { type: String, default: '' },
        /** 小标签的语义方向：up = 向好（绿），down = 变差（红） */
        tagType: { type: String, default: 'up' },
        /** 超过 1 万时是否紧凑显示（1.2万） */
        compactMode: { type: Boolean, default: false }
    },
    computed: {
        displayValue() {
            const n = Number(this.value)
            if (!isFinite(n)) return this.value
            if (this.compactMode && Math.abs(n) >= 10000) return compact(n)
            return thousand(n)
        },
        iconBg() {
            return this.hexToRgba(this.accent, 0.12)
        },
        tagColor() {
            return this.tagType === 'down' ? '#B8431A' : '#1F7A4D'
        },
        tagBg() {
            return this.tagType === 'down' ? 'rgba(214,69,61,.10)' : 'rgba(47,163,107,.12)'
        }
    },
    methods: {
        hexToRgba(hex, alpha) {
            const h = (hex || '').replace('#', '')
            if (h.length !== 6) return 'transparent'
            const r = parseInt(h.slice(0, 2), 16)
            const g = parseInt(h.slice(2, 4), 16)
            const b = parseInt(h.slice(4, 6), 16)
            return 'rgba(' + r + ',' + g + ',' + b + ',' + alpha + ')'
        }
    }
}
</script>

<style scoped>
.kpi {
    position: relative;
    padding: 14px 16px 13px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.kpi__mark {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
}

.kpi__hd {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
}

.kpi__label {
    font-size: 12.5px;
    color: var(--csu-text-sub);
    white-space: nowrap;
}

.kpi__icon {
    width: 26px;
    height: 26px;
    border-radius: 7px;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.kpi__value {
    display: flex;
    align-items: baseline;
    min-height: 30px;
}

.kpi__ft {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-height: 17px;
}

.kpi__hint {
    font-size: 11.5px;
    color: var(--csu-text-weak);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.kpi__tag {
    flex: none;
    font-size: 11px;
    font-weight: 600;
    padding: 1px 6px;
    border-radius: 9px;
    font-family: var(--csu-font-mono);
}
</style>
