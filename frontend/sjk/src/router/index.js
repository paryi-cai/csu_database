import Vue from 'vue'
import VueRouter from 'vue-router'
import LogRes from '@/components/MyLogReg'
import user from '@/components/MyUser'
import manage from '@/components/MyManage'
Vue.use(VueRouter)
const router = new VueRouter({
    mode:'history',
    routes: [
        {
            path:'/',
            redirect:'/login'
        },
        {
            path: '/login',
            component: LogRes,
            meta: {
                title: "登录"
            },
        },
        {
            path: '/user',
            component: user,
            meta: {
                title: "用户界面"
            }
        },
        {
            path: '/manage',
            component: manage,
            meta: {
                title: "后台管理界面"
            }
        },
    ]
})

// 用路由 meta.title 更新浏览器标签页标题：
// index.html 的默认标题是 webpack 占位（"sjk"），不处理的话所有标签页长得一模一样。
router.afterEach((to) => {
    const title = (to.meta && to.meta.title) || '首页'
    document.title = title + ' · 校园快餐订餐管理系统'
})

export default router