const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    // 路由使用 history 模式，直接访问 /manage、/user 需要 dev server 回落到 index.html。
    // @vue/cli 5 默认的 historyApiFallback 已经能处理，这里显式写出来是为了
    // 顺手把注册页也指到登录页：该页依赖的短信验证码接口未实现（404），
    // 页面已下线，但历史书签/外部链接仍可能落在 /register 上。
    historyApiFallback: {
      rewrites: [
        { from: /^\/register/, to: '/index.html' },
        { from: /./, to: '/index.html' }
      ]
    }
  }
})
