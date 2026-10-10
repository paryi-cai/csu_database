<template>
    <page-panel title="逛店铺" subtitle="fastfood_shop 表 · 单价来自数据库，销量由触发器维护" accent="#E8622C">
        <template slot="actions">
            <el-input v-model="filter.keyword" class="filter-input" size="small" clearable prefix-icon="el-icon-search"
                placeholder="搜索店铺名称"></el-input>
            <el-select v-model="sortBy" size="small" class="filter-select">
                <el-option value="default" label="默认排序"></el-option>
                <el-option value="priceAsc" label="单价从低到高"></el-option>
                <el-option value="priceDesc" label="单价从高到低"></el-option>
                <el-option value="saleDesc" label="销量从高到低"></el-option>
            </el-select>
            <el-button size="small" icon="el-icon-refresh" @click="getdata">刷新</el-button>
            <el-badge :value="cartCount" :hidden="cartCount === 0" class="cart-badge">
                <el-button size="small" type="primary" icon="el-icon-shopping-cart-2" @click="cartVisible = true">
                    购物车
                </el-button>
            </el-badge>
        </template>

        <!-- 店铺卡片网格：比原来的表格更适合"逛"，一屏能看到更多店铺的关键信息 -->
        <div v-loading="loading" class="shop-grid">
            <div v-for="row in sortedList" :key="row.shop_name" class="shop-card csu-card csu-card--hover">
                <div class="shop-card__hd">
                    <span class="shop-card__name" :title="row.shop_name">{{ row.shop_name }}</span>
                    <span class="shop-card__price">
                        <b class="csu-num">{{ money(row.price) }}</b><em>元/份</em>
                    </span>
                </div>

                <div class="shop-card__meta">
                    <span>销量 <b class="csu-num">{{ num(row.sale) }}</b> 单</span>
                    <span>约 <b class="csu-num">{{ money(row.price * row.sale) }}</b> 元</span>
                </div>

                <div class="shop-card__ft">
                    <el-button size="mini" type="primary" plain icon="el-icon-plus" @click="addToCart(row)">
                        加入购物车
                    </el-button>
                    <el-button size="mini" type="text" @click="orderOne(row)">立即下单</el-button>
                </div>
            </div>
        </div>

        <div v-if="!loading && !sortedList.length" class="shop-empty">
            <i class="el-icon-dish"></i>
            <p v-if="keywordTrimmed">没有匹配「{{ keywordTrimmed }}」的店铺</p>
            <p v-else>暂时没有可点的店铺</p>
        </div>

        <div class="csu-hint list-ft">
            共 {{ sortedList.length }} 家店铺 · 点击「加入购物车」可一次下单多份，结算时按店铺分别生成订单
        </div>

        <!-- 购物车抽屉 -->
        <el-drawer title="购物车" :visible.sync="cartVisible" direction="rtl" size="420px">
            <div v-if="!cart.length" class="cart-empty">
                <i class="el-icon-shopping-cart-2"></i>
                <p>购物车还是空的</p>
            </div>
            <div v-else class="cart">
                <div v-for="group in cartGroups" :key="group.shopName" class="cart-group">
                    <div class="cart-group__hd">
                        <span class="cart-group__name">{{ group.shopName }}</span>
                        <span class="cart-group__sub">
                            {{ money(group.price) }} 元/份 × {{ group.qty }} = <b class="csu-num">{{ money(group.subtotal) }}</b> 元
                        </span>
                    </div>
                    <div class="cart-group__ops">
                        <el-button size="mini" icon="el-icon-minus" circle @click="changeQty(group.shopName, -1)">
                        </el-button>
                        <span class="cart-qty csu-num">{{ group.qty }}</span>
                        <el-button size="mini" icon="el-icon-plus" circle @click="changeQty(group.shopName, 1)">
                        </el-button>
                        <el-button size="mini" type="text" class="cart-del" @click="removeShop(group.shopName)">
                            移除
                        </el-button>
                    </div>
                </div>

                <div class="cart-total">
                    <span>合计</span>
                    <b class="csu-num">{{ money(cartTotal) }}</b>
                    <em>元</em>
                </div>
            </div>

            <div class="cart-ft">
                <el-button size="small" @click="clearCart" :disabled="!cart.length">清空</el-button>
                <el-button size="small" type="primary" :disabled="!cart.length" @click="openCheckout">
                    去结算（{{ cartCount }} 份）
                </el-button>
            </div>
        </el-drawer>

        <!-- 结算表单 -->
        <el-dialog title="确认下单" :visible.sync="dialog" width="440px" :close-on-click-modal="false">
            <div class="checkout-head">
                <div v-for="group in cartGroups" :key="group.shopName" class="checkout-line">
                    <span>{{ group.shopName }}</span>
                    <span class="csu-num">{{ group.qty }} 份 · {{ money(group.subtotal) }} 元</span>
                </div>
                <div class="checkout-total">
                    合计 <b class="csu-num">{{ money(cartTotal) }}</b> 元
                    <em v-if="cartGroups.length > 1">（将按店铺拆成 {{ cartGroups.length }} 笔订单）</em>
                </div>
                <div class="csu-hint checkout-note">
                    说明：订单金额按「份数 × 单价」的小计提交。本项目 oorder 表没有数量列，
                    下单接口也不接受份数（既有契约），因此份数只体现在金额上；
                    数据库的 m_sale_v 触发器按"订单条数"累加，不按份数。
                </div>
            </div>

            <el-form ref="form" :model="form" label-width="90px" :rules="formRules">
                <el-form-item label="订餐方式：" prop="order_way">
                    <el-select v-model="form.order_way" placeholder="请选择订餐方式" style="width: 100%">
                        <el-option v-for="way in orderWays" :key="way" :label="way" :value="way"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="顾客姓名：" prop="cons_name">
                    <el-input v-model="form.cons_name" placeholder="请填写取餐人姓名"></el-input>
                </el-form-item>
                <el-form-item label="送餐地址：" prop="cons_addre">
                    <el-input v-model="form.cons_addre" placeholder="例如 15公寓"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer">
                <el-button size="small" @click="dialog = false">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="submit">
                    确认下单
                </el-button>
            </div>
        </el-dialog>
    </page-panel>
</template>

<script>
import PagePanel from '@/components/common/PagePanel.vue'
import { fmtMoney, fmtNum, ORDER_WAYS } from '@/utils/format'

/**
 * 逛店铺 + 下单。
 *
 * 相对原实现的变化：
 *   1. 表格改为店铺卡片网格，单价/销量/参考营业额一眼可见（原表格只有三列文字）；
 *   2. 增加购物车：可以一次加多份、跨店铺选择，结算时按店铺分别调用下单接口
 *      —— 因为 oorder 一条记录只对应一个店铺，跨店铺必须拆单，这一点在界面上明确提示；
 *   3. 增加关键字搜索与排序；下单表单加校验（原来 cons_name / cons_addre 可以为空）。
 *
 * 下单接口完全沿用：POST /api/user/addorder，字段 shop_name / order_money /
 * order_way / cons_name / cons_addre；顾客手机号由后端从 JWT 取，前端不传。
 */
export default {
    components: { PagePanel },
    created() {
        this.getdata()
    },
    data() {
        return {
            loading: false,
            submitting: false,
            rawList: [],
            filter: { keyword: '' },
            sortBy: 'default',
            cart: [],
            cartVisible: false,
            dialog: false,
            orderWays: ORDER_WAYS,
            form: {
                order_way: '人工订餐',
                cons_name: '',
                cons_addre: ''
            },
            formRules: {
                order_way: [{ required: true, message: '请选择订餐方式', trigger: 'change' }],
                cons_name: [{ required: true, message: '请填写取餐人姓名', trigger: 'blur' }],
                cons_addre: [{ required: true, message: '请填写送餐地址', trigger: 'blur' }]
            }
        }
    },
    computed: {
        keywordTrimmed() {
            const raw = this.filter.keyword || ''
            return String(raw).trim()
        },
        filteredList() {
            const keyword = this.keywordTrimmed.toLowerCase()
            if (!keyword) return this.rawList
            return this.rawList.filter((row) => String(row.shop_name || '').toLowerCase().indexOf(keyword) >= 0)
        },
        sortedList() {
            const list = this.filteredList.slice()
            const by = this.sortBy
            if (by === 'priceAsc') list.sort((a, b) => a.price - b.price)
            else if (by === 'priceDesc') list.sort((a, b) => b.price - a.price)
            else if (by === 'saleDesc') list.sort((a, b) => b.sale - a.sale)
            return list
        },
        cartCount() {
            return this.cart.reduce((sum, item) => sum + item.qty, 0)
        },
        /** 按店铺归并，用于结算（一店一单） */
        cartGroups() {
            const map = {}
            this.cart.forEach((item) => {
                if (!map[item.shop_name]) {
                    map[item.shop_name] = { shopName: item.shop_name, price: item.price, qty: 0, subtotal: 0 }
                }
                map[item.shop_name].qty += item.qty
                map[item.shop_name].subtotal = map[item.shop_name].qty * item.price
            })
            return Object.keys(map).map((key) => map[key])
        },
        cartTotal() {
            return this.cartGroups.reduce((sum, group) => sum + group.subtotal, 0)
        }
    },
    methods: {
        num(v) {
            return fmtNum(v)
        },
        money(v) {
            return fmtMoney(v)
        },
        getdata() {
            this.loading = true
            this.$axios
                .get('/api/user/shop')
                .then((res) => {
                    if (res.data.status == 200) {
                        this.rawList = res.data.tabledata || []
                    }
                })
                .catch(() => this.$message({ message: '网络故障，店铺列表加载失败', type: 'error' }))
                .finally(() => {
                    this.loading = false
                })
        },
        /* ==================== 购物车 ==================== */
        addToCart(row) {
            const exist = this.cart.find((item) => item.shop_name === row.shop_name)
            if (exist) {
                exist.qty += 1
            } else {
                this.cart.push({ shop_name: row.shop_name, price: row.price, qty: 1 })
            }
            this.$message({ message: '已加入购物车：' + row.shop_name, type: 'success' })
        },
        changeQty(shopName, delta) {
            const item = this.cart.find((row) => row.shop_name === shopName)
            if (!item) return
            item.qty += delta
            if (item.qty <= 0) this.removeShop(shopName)
        },
        removeShop(shopName) {
            this.cart = this.cart.filter((item) => item.shop_name !== shopName)
        },
        clearCart() {
            this.cart = []
        },
        orderOne(row) {
            // "立即下单"：把该店铺以 1 份写入购物车后直接结算
            const exist = this.cart.find((item) => item.shop_name === row.shop_name)
            if (!exist) this.cart.push({ shop_name: row.shop_name, price: row.price, qty: 1 })
            this.openCheckout()
        },
        openCheckout() {
            if (!this.cart.length) {
                this.$message({ message: '购物车是空的', type: 'warning' })
                return
            }
            this.cartVisible = false
            this.dialog = true
            this.$nextTick(() => {
                if (this.$refs.form) this.$refs.form.clearValidate()
            })
        },
        /* ==================== 下单 ==================== */
        /**
         * 提交订单。
         *
         * 两个容易搞错的点：
         *  1. {@code order_money} 必须传该店铺的**小计**（qty × 单价），不是单价——
         *     原来传的是 group.price，选 3 份时结算页显示 45 元、入库却只有 15 元。
         *     本项目 oorder 没有"数量"列、提交接口也不接受份数（既有契约不能改），
         *     所以份数只能体现在金额上（见结算弹窗里的说明）。
         *  2. 每一笔都自行 catch 成"这一笔失败"的对象，再交给 {@code Promise.all}：
         *     跨店铺下单是多个独立请求，其中一笔网络失败时不能把已成功的订单一起当失败，
         *     否则购物车不清空，用户重试会对已成功的店铺重复下单。
         *     这里刻意不用 {@code Promise.allSettled}：它在编译产物里是裸调用，
         *     项目的 babel/browserslist 配置并没有为它注入 core-js polyfill，
         *     而"每笔先 catch 再 all"的写法不依赖任何新 API，语义完全等价。
         */
        submit() {
            this.$refs.form.validate((valid) => {
                if (!valid) return
                this.submitting = true

                const tasks = this.cartGroups.map((group) =>
                    this.$axios
                        .post('/api/user/addorder', {
                            shop_name: group.shopName,
                            // 小计，不是单价
                            order_money: group.subtotal,
                            order_way: this.form.order_way,
                            cons_name: this.form.cons_name,
                            cons_addre: this.form.cons_addre
                        })
                        .then((res) => ({ group: group, ok: res.data.status == 200, msg: res.data.msg }))
                        .catch(() => ({ group: group, ok: false, msg: '网络故障' }))
                )

                Promise.all(tasks)
                    .then((results) => {
                        const okGroups = results.filter((r) => r.ok).map((r) => r.group)
                        const failedGroups = results.filter((r) => !r.ok)

                        // 只从购物车里剔除已成功的店铺，失败的原样保留供用户重试
                        okGroups.forEach((group) => this.removeShop(group.shopName))

                        if (!failedGroups.length) {
                            this.$message({
                                message: okGroups.length > 1 ? '成功下单 ' + okGroups.length + ' 笔' : '成功下单',
                                type: 'success'
                            })
                            this.dialog = false
                        } else if (okGroups.length) {
                            this.$message({
                                message:
                                    '已成功 ' + okGroups.length + ' 笔；' +
                                    failedGroups.map((r) => r.group.shopName).join('、') +
                                    ' 下单失败，已保留在购物车，可重试',
                                type: 'warning',
                                duration: 6000
                            })
                        } else {
                            this.$message({ message: '下单失败：' + (failedGroups[0].msg || '请稍后重试'), type: 'error' })
                        }
                        this.getdata()
                    })
                    .finally(() => {
                        // 上面的 map 已保证每笔都不会 reject，因此这里不会再出现"漏网的失败"
                        this.submitting = false
                    })
            })
        }
    },
    watch: {
        cartCount(val) {
            // 通知外壳刷新顶部购物车角标
            this.$emit('cart-change', val)
        }
    }
}
</script>

<style scoped>
.filter-input {
    width: 200px;
}

.filter-select {
    width: 150px;
}

.cart-badge {
    margin-left: 4px;
}

.shop-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
    gap: 14px;
    min-height: 80px;
}

.shop-card {
    padding: 14px;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.shop-card__hd {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
}

.shop-card__name {
    font-size: 14.5px;
    font-weight: 600;
    color: var(--csu-text);
    line-height: 20px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.shop-card__price {
    flex: none;
    color: var(--csu-primary);
    white-space: nowrap;
}

.shop-card__price b {
    font-size: 19px;
    font-weight: 700;
}

.shop-card__price em {
    font-style: normal;
    font-size: 11px;
    color: var(--csu-text-weak);
    margin-left: 2px;
}

.shop-card__meta {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    font-size: 12px;
    color: var(--csu-text-weak);
    padding-top: 8px;
    border-top: 1px dashed var(--csu-border);
}

.shop-card__meta b {
    color: var(--csu-text-sub);
    font-weight: 600;
}

.shop-card__ft {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: auto;
}

.shop-empty {
    padding: 40px 0;
    text-align: center;
    color: var(--csu-text-weak);
}

.shop-empty i {
    font-size: 38px;
    color: var(--csu-icon-empty);
}

.shop-empty p {
    margin: 10px 0 0;
    font-size: 13px;
}

.list-ft {
    margin-top: 14px;
}

/* ---------- 购物车 ---------- */
.cart-empty {
    padding: 60px 0;
    text-align: center;
    color: var(--csu-text-weak);
}

.cart-empty i {
    font-size: 40px;
    color: var(--csu-icon-empty);
}

.cart-empty p {
    margin-top: 10px;
}

.cart-group {
    padding: 12px 0;
    border-bottom: 1px dashed var(--csu-border);
}

.cart-group__hd {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 8px;
}

.cart-group__name {
    font-weight: 600;
    color: var(--csu-text);
}

.cart-group__sub {
    font-size: 12px;
    color: var(--csu-text-weak);
}

.cart-group__sub b {
    color: var(--csu-primary-text);
}

.cart-group__ops {
    display: flex;
    align-items: center;
    gap: 8px;
}

.cart-qty {
    min-width: 26px;
    text-align: center;
    font-weight: 700;
    color: var(--csu-text);
}

.cart-del {
    margin-left: auto;
    color: var(--csu-danger) !important;
}

.cart-total {
    display: flex;
    align-items: baseline;
    justify-content: flex-end;
    gap: 4px;
    padding: 14px 0 4px;
    font-size: 13px;
    color: var(--csu-text-sub);
}

.cart-total b {
    font-size: 22px;
    font-weight: 700;
    color: var(--csu-primary);
}

.cart-total em {
    font-style: normal;
    font-size: 12px;
    color: var(--csu-text-weak);
}

.cart-ft {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding-top: 12px;
    border-top: 1px solid var(--csu-border-light);
}

.checkout-head {
    background: var(--csu-panel-alt);
    border-radius: var(--csu-radius-sm);
    padding: 10px 12px;
    margin-bottom: 16px;
}

.checkout-line {
    display: flex;
    justify-content: space-between;
    font-size: 13px;
    color: var(--csu-text-sub);
    line-height: 1.9;
}

.checkout-total {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-top: 6px;
    padding-top: 6px;
    border-top: 1px dashed var(--csu-border);
    font-size: 13px;
    color: var(--csu-text-sub);
}

.checkout-total b {
    font-size: 19px;
    font-weight: 700;
    color: var(--csu-primary);
}

.checkout-total em {
    font-style: normal;
    font-size: 12px;
    color: var(--csu-text-weak);
}

.checkout-note {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed var(--csu-border);
    line-height: 1.7;
}
</style>
