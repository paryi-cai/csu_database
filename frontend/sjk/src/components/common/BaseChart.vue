<template>
    <div ref="holder" class="csu-chart" :style="{ height: height }"></div>
</template>

<script>
/**
 * ECharts 通用容器组件（防泄漏 + 自适应 + 空态兜底）。
 *
 * 关键设计：
 *  1. 实例存在组件私有字段（_chart）而不是 data 里：Vue 2 会递归劫持 data 上的对象，
 *     ECharts 实例内部结构庞大且带自引用，放进 data 会产生无意义的响应式开销。
 *  2. resize 用 window 监听 + 全局订阅表，而不是每个图表各自 addEventListener：
 *     一个页面 8 张图就是 8 个监听器，窗口拖动时会重复触发。
 *  3. 在此之上再用 ResizeObserver 观察**容器自身**：侧栏折叠之类的布局变化不会触发
 *     window resize，只有容器尺寸变了，靠 window 监听会漏掉（画布保持旧宽度）。
 *  3. dispose 在 beforeDestroy 中执行并解除订阅；setOption 前统一判空，
 *     避免请求返回晚于组件销毁时的典型内存泄漏与报错。
 *  4. v-show 隐藏时容器高宽为 0，ECharts 会画出 0 尺寸画布；
 *     因此父组件切回本页时调用 activate()，或用 visible 属性驱动。
 */
import echarts, { THEME_NAME } from '@/charts/echarts'

/** 全局 resize 订阅表：一个 window 监听服务所有图表实例 */
const subscribers = new Set()
let listening = false

function handleWindowResize() {
    subscribers.forEach((fn) => fn())
}

function subscribe(fn) {
    subscribers.add(fn)
    if (!listening) {
        window.addEventListener('resize', handleWindowResize)
        listening = true
    }
}

function unsubscribe(fn) {
    subscribers.delete(fn)
    if (listening && subscribers.size === 0) {
        window.removeEventListener('resize', handleWindowResize)
        listening = false
    }
}

export default {
    name: 'BaseChart',
    props: {
        /** ECharts option，变化时整体重设（notMerge=false，保留用户的缩放/选中状态） */
        option: {
            type: Object,
            required: true
        },
        /** 容器高度，默认 300px */
        height: {
            type: String,
            default: '300px'
        },
        /** 是否处于可见状态；从 false 变 true 时补一次 resize */
        visible: {
            type: Boolean,
            default: true
        },
        /** 骨架屏：请求未完成时用浅色占位，避免出现"空白卡片" */
        loading: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {}
    },
    watch: {
        option: {
            deep: true,
            handler() {
                this.render()
            }
        },
        visible(val) {
            if (val) {
                this.$nextTick(() => this.resize())
            }
        },
        loading(val) {
            // 加载结束后必须"补渲染 + 补 resize"：
            // render() 在 loading 期间是直接 return 的，如果某次数据更新恰好发生在
            // loading 仍为 true 的时候被跳过，只 resize 会留下一张空白图。
            // （当前 8 个调用方都是"先改数据、后关 loading"，所以碰不到；
            //   这里做成自防御，避免以后新增图表时踩这个时序。）
            if (!val) {
                this.$nextTick(() => {
                    this.render()
                    this.resize()
                })
            }
        }
    },
    mounted() {
        this.init()
    },
    beforeDestroy() {
        this.destroy()
    },
    methods: {
        init() {
            if (this._chart || !this.$refs.holder) return
            this._chart = echarts.init(this.$refs.holder, THEME_NAME, { renderer: 'canvas' })
            this._chart.on('click', this.onChartClick)
            this._resizeHandler = () => this.resize()
            subscribe(this._resizeHandler)
            // 观察**容器自身**尺寸变化：仅仅监听 window resize 是不够的——
            // 侧栏折叠（宽度 240px → 64px）会让内容区变宽，但 window 尺寸没变，
            // 于是图表画布保持旧宽度，出现留白或溢出。同样受影响的还有栅格换列、
            // 抽屉展开等一切"窗口没变但容器变了"的场景。
            // ResizeObserver 在支持它的浏览器上直接覆盖这些情况；不支持时（老浏览器）
            // 退化成上面的 window 监听，不会更差。
            if (typeof ResizeObserver !== 'undefined') {
                // 用 requestAnimationFrame 合并同一帧内的多次回调：
                // 侧栏折叠带 0.2s 的 width 过渡，ResizeObserver 会在动画期间连续触发，
                // 其中某些帧容器宽度可能瞬时为 0（resize() 会跳过，但这样就漏掉了最终尺寸）。
                // 合并到下一帧再执行，等价于"布局稳定后测一次"。
                this._observer = new ResizeObserver(() => {
                    if (this._rafPending) return
                    this._rafPending = true
                    requestAnimationFrame(() => {
                        this._rafPending = false
                        this.resize()
                    })
                })
                this._observer.observe(this.$refs.holder)
            }
            this.render()
        },
        render() {
            if (!this._chart || this._chart.isDisposed()) return
            if (this.loading) return
            // notMerge = false：折线图 dataZoom、饼图选中态在数据刷新后保留
            this._chart.setOption(this.option || {}, false, true)
        },
        resize() {
            if (!this._chart || this._chart.isDisposed()) return
            const el = this.$refs.holder
            // 隐藏状态下宽高为 0，此时 resize 会让画布彻底塌掉，直接跳过
            if (!el || el.offsetWidth === 0 || el.offsetHeight === 0) return
            this._chart.resize()
        },
        destroy() {
            if (this._observer) {
                this._observer.disconnect()
                this._observer = null
            }
            if (this._resizeHandler) {
                unsubscribe(this._resizeHandler)
                this._resizeHandler = null
            }
            if (this._chart) {
                this._chart.off('click', this.onChartClick)
                if (!this._chart.isDisposed()) this._chart.dispose()
                this._chart = null
            }
        },
        onChartClick(params) {
            // 只把必要信息抛给父组件，父组件不需要感知 ECharts 的 params 结构
            this.$emit('chart-click', {
                name: params.name,
                value: params.value,
                seriesName: params.seriesName,
                dataIndex: params.dataIndex,
                data: params.data,
                raw: params
            })
        },
        getInstance() {
            return this._chart
        }
    }
}
</script>

<style scoped>
.csu-chart {
    width: 100%;
}
</style>
