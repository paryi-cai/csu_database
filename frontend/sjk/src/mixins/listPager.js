/**
 * 列表页的「筛选 + 分页」混入。
 *
 * 背景：本项目的列表接口（/api/manager/unsend、/api/user/unsend 等）按契约
 * 都是<b>全量返回 tabledata</b>，响应结构不能改。而订单表在演示数据下有 358 行，
 * 一次性铺在表格里既不好看也不好用。
 *
 * 因此分页与筛选放在前端做：接口不变、请求次数不变（都是 1 次），
 * 只是把渲染行数收敛到 pageSize。对数据量小的页面（店铺/服务员/配送员）
 * 把 pageSize 设为 0 即关闭分页。
 *
 * 使用方式：
 *   mixins: [listPager]
 *   data () { return { rawList: [], filter: { keyword: '' }, page: 1, pageSize: 10 } }
 *   computed: { filterFields () { return ['order_id', 'cons_phone', 'cons_name'] } }
 *   模板：<el-table :data="pageData"> + <el-pagination v-if="paged" ... />
 *
 * 需要额外筛选维度时，覆盖 computed filtered 并复用 applyKeywordFilter()：
 *   computed: {
 *     filtered () {
 *       const base = this.applyKeywordFilter()
 *       return this.filter.shop ? base.filter(r => r.shop === this.filter.shop) : base
 *     }
 *   }
 */
export default {
    data() {
        return {
            page: 1,
            /** 0 表示不分页 */
            pageSize: 0
        }
    },
    computed: {
        /** 需要在其中做关键字匹配的字段，由页面覆盖 */
        filterFields() {
            return []
        },
        /** 过滤关键字（去空格）；页面提供 filter.keyword */
        keywordTrimmed() {
            const raw = this.filter && this.filter.keyword ? this.filter.keyword : ''
            return String(raw).trim()
        },
        /** 是否真的分页 */
        paged() {
            return this.pageSize > 0
        },
        /** 过滤后的完整结果（默认只有关键字这一维，页面可覆盖） */
        filtered() {
            return this.applyKeywordFilter()
        },
        /** 当前页数据；未开启分页时等同于 filtered */
        pageData() {
            if (!this.paged) return this.filtered
            const start = (this.page - 1) * this.pageSize
            return this.filtered.slice(start, start + this.pageSize)
        },
        total() {
            return this.filtered.length
        }
    },
    watch: {
        // 关键字变化后必须回到第一页，否则会停在越界页码上看到空表
        keywordTrimmed() {
            this.page = 1
        }
    },
    methods: {
        /**
         * 关键字过滤（供页面在覆盖 filtered 时复用）。
         * 抽成方法而不是让页面去取 mixin 的 computed，避免依赖 $options.mixins 的内部结构。
         */
        applyKeywordFilter() {
            const list = Array.isArray(this.rawList) ? this.rawList : []
            const keyword = this.keywordTrimmed
            if (!keyword) return list

            const lower = keyword.toLowerCase()
            const fields = this.filterFields
            return list.filter((row) => {
                if (!row) return false
                return fields.some((field) => {
                    const value = row[field]
                    return value !== null && value !== undefined &&
                        String(value).toLowerCase().indexOf(lower) >= 0
                })
            })
        },
        handlePageChange(page) {
            this.page = page
        },
        /** 数据刷新后校正页码（例如删掉最后一条记录后当前页越界） */
        clampPage() {
            if (!this.paged) return
            const maxPage = Math.max(1, Math.ceil(this.total / this.pageSize))
            if (this.page > maxPage) this.page = maxPage
        }
    }
}
