/**
 * 应用装配与启动。
 *
 * 单独抽成模块（而不是全写在 main.js 里）的原因：模板编译、插件注册、根实例创建
 * 集中在一处，便于按需复用——例如本地对某个组件做"单独渲染"排查时，可以直接
 * import { createApp } 自行挂载该组件，不必把 main.js 的副作用再跑一遍。
 */
import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
// 设计 token（配色/圆角/间距）与 Element 主题覆写，必须在 element-ui 样式之后引入
import '@/assets/styles/theme.css'
import router from '@/router'
import App from '@/App.vue'

import axios from '@/api/index'

// 图表页面与列表页的组件都依赖以下全局能力，统一在这里注册
Vue.use(ElementUI)
Vue.config.productionTip = false
// 修改 vue 的原型链，保持既有页面的 this.$axios 用法不变
Vue.prototype.$axios = axios

/**
 * 创建并挂载根实例。
 *
 * @param {string} selector 挂载点，默认项目既有的 #app
 * @returns {Vue} 根实例
 */
export function createApp(selector = '#app') {
    const app = new Vue({
        axios,
        router,
        render: (h) => h(App)
    })
    app.$mount(selector)
    return app
}

export default createApp
