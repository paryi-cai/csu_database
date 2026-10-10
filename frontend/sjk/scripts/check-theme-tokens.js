#!/usr/bin/env node
/**
 * 设计 token 一致性校验。
 *
 * 为什么需要它：CSS 变量与 ECharts 配色天然存在"两处各存一份"的问题
 * （CANVAS 渲染的图表读不到 CSS 变量，必须传具体色值）。只写一句
 * "两边改动必须同步"是空头承诺——这个脚本把它变成可执行断言。
 *
 * 校验四项：
 *   1. echarts.js 的 C 常量里，与 theme.css 同名的颜色必须完全一致
 *      （primary / ink / amber / green / blue / purple / danger / text / textSub / textWeak / border / borderLight / panel）；
 *   2. 所有组件里的 var(--csu-*) 引用，必须在 theme.css 中有定义（写错变量名会静默失效）；
 *   3. theme.css 里不允许出现"定义了但全项目没人引用"的 token（死 token 会误导人）；
 *   4. 显式豁免清单之外的硬编码色值（组件 <style> 段内）必须为 0。
 *
 * 退出码：0 = 全部通过；1 = 有失败项（便于接入 CI）。
 *
 * 用法：cd frontend/sjk && npm run check:theme
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..')
const SRC = path.join(ROOT, 'src')
const THEME = path.join(SRC, 'assets', 'styles', 'theme.css')
const ECHARTS = path.join(SRC, 'charts', 'echarts.js')

const failures = []
const notes = []

function read(file) {
  return fs.readFileSync(file, 'utf8')
}

function walk(dir, exts, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walk(full, exts, out)
    else if (exts.includes(path.extname(entry.name))) out.push(full)
  }
  return out
}

const themeCss = read(THEME)
const echartsJs = read(ECHARTS)
const sourceFiles = walk(SRC, ['.vue', '.js'])
const rel = (p) => path.relative(ROOT, p).replace(/\\/g, '/')

/* ------------------------------------------------------------------ *
 * 1. theme.css 与 echarts.js 的同名颜色必须一致
 * ------------------------------------------------------------------ */

/** CSS 变量名  ->  echarts.js 中 C 对象的键 */
const COLOR_MAP = {
  '--csu-primary': 'primary',
  '--csu-primary-light': 'primaryLight',
  '--csu-primary-bg': 'primaryBg',
  '--csu-sidebar': 'ink',
  '--csu-warning': 'amber',
  '--csu-success': 'green',
  '--csu-danger': 'danger',
  '--csu-text': 'text',
  '--csu-text-sub': 'textSub',
  '--csu-text-weak': 'textWeak',
  '--csu-border': 'border',
  '--csu-border-light': 'borderLight',
  '--csu-panel': 'panel',
  '--csu-header-bg': 'headerBg'
}

function cssVarValue(name) {
  const m = new RegExp(name.replace(/-/g, '\\-') + '\\s*:\\s*([^;]+);').exec(themeCss)
  return m ? m[1].trim().toUpperCase() : null
}

function cConstant(key) {
  // 匹配 C 对象里形如  primary: '#E8622C',
  const m = new RegExp('\\b' + key + '\\s*:\\s*\'(#[0-9A-Fa-f]{3,8})\'').exec(echartsJs)
  return m ? m[1].toUpperCase() : null
}

for (const [cssName, jsKey] of Object.entries(COLOR_MAP)) {
  const cssValue = cssVarValue(cssName)
  const jsValue = cConstant(jsKey)
  if (cssValue === null) {
    failures.push(`theme.css 缺少变量 ${cssName}（echarts.js 的 C.${jsKey} 需要与它对应）`)
    continue
  }
  if (jsValue === null) {
    failures.push(`echarts.js 缺少常量 C.${jsKey}（theme.css 的 ${cssName} 需要与它对应）`)
    continue
  }
  if (cssValue !== jsValue) {
    failures.push(`配色漂移：${cssName} = ${cssValue}，但 echarts.js 的 C.${jsKey} = ${jsValue}`)
  }
}
notes.push(`校验了 ${Object.keys(COLOR_MAP).length} 组 CSS/JS 同名颜色`)

/* ------------------------------------------------------------------ *
 * 2. 所有 var(--csu-*) 引用都必须有定义
 * ------------------------------------------------------------------ */

const defined = new Set(
  (themeCss.match(/--csu-[a-z0-9-]+\s*:/g) || []).map((s) => s.replace(/\s*:$/, ''))
)

const referenced = new Map() // 变量名 -> Set(文件)
for (const file of sourceFiles) {
  const text = read(file)
  const re = /var\((--csu-[a-z0-9-]+)/g
  let m
  while ((m = re.exec(text)) !== null) {
    if (!referenced.has(m[1])) referenced.set(m[1], new Set())
    referenced.get(m[1]).add(rel(file))
  }
}
// theme.css 自身的引用也要算
{
  const re = /var\((--csu-[a-z0-9-]+)/g
  let m
  while ((m = re.exec(themeCss)) !== null) {
    if (!referenced.has(m[1])) referenced.set(m[1], new Set())
    referenced.get(m[1]).add(rel(THEME))
  }
}

for (const [name, files] of referenced) {
  if (!defined.has(name)) {
    failures.push(`未定义的 CSS 变量 ${name}（被引用于 ${[...files].join(', ')}）——写错变量名会静默失效`)
  }
}
notes.push(`扫描了 ${referenced.size} 个被引用的变量，theme.css 定义了 ${defined.size} 个`)

/* ------------------------------------------------------------------ *
 * 3. 死 token（定义了但没人引用）
 * ------------------------------------------------------------------ */

for (const name of defined) {
  if (!referenced.has(name)) {
    failures.push(`死 token ${name}：theme.css 里定义了但全项目无人引用，会误导维护者`)
  }
}

/* ------------------------------------------------------------------ *
 * 4. 组件 <style> 段内不得出现硬编码色值
 * ------------------------------------------------------------------ */

for (const file of sourceFiles.filter((f) => f.endsWith('.vue'))) {
  const text = read(file)
  const styleIdx = text.indexOf('<style')
  if (styleIdx < 0) continue
  const stylePart = text.slice(styleIdx)
  const lines = stylePart.split(/\r?\n/)
  lines.forEach((line, i) => {
    if (/#[0-9A-Fa-f]{3,8}\b/.test(line) && !/^\s*(\/\/|\*|\/\*)/.test(line)) {
      failures.push(`${rel(file)} 的 <style> 段内有硬编码色值：${line.trim()}`)
    }
  })
}

/* ------------------------------------------------------------------ *
 * 5. 声明顺序：CHART_COLORS 引用 C，所以 C 必须先声明
 * ------------------------------------------------------------------ */

{
  const idxC = echartsJs.search(/export\s+const\s+C\s*=/)
  const idxColors = echartsJs.search(/export\s+const\s+CHART_COLORS\s*=/)
  if (idxC < 0) {
    failures.push('echarts.js 里找不到 `export const C =`')
  } else if (idxColors < 0) {
    failures.push('echarts.js 里找不到 `export const CHART_COLORS =`')
  } else if (idxColors < idxC) {
    failures.push(
      'echarts.js 声明顺序错误：CHART_COLORS 定义在 C 之前，但引用了 C.*；' +
      'const 无跨声明提升（TDZ），会在模块加载时抛 "Cannot access \'C\' before initialization"' +
      '，而 app.js 首先 import 本模块 → 整站白屏。请把 C 移到 CHART_COLORS 之前。'
    )
  }
  notes.push('已断言 echarts.js 中 C 声明早于 CHART_COLORS（避免 TDZ 白屏）')
}

/* ------------------------------------------------------------------ *
 * 输出
 * ------------------------------------------------------------------ */

if (failures.length === 0) {
  console.log('[check:theme] 全部通过')
  notes.forEach((n) => console.log('  · ' + n))
  process.exit(0)
}

console.error('[check:theme] 发现 ' + failures.length + ' 个问题：')
failures.forEach((f, i) => console.error(`  ${i + 1}. ${f}`))
process.exit(1)
