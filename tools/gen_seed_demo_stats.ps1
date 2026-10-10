# 生成「统计数据演示数据」SQL 的脚本（只生成 SQL 文件，不连接数据库、不改动任何结构）。
#
# 用途：init.sql / caimingxuan.sql 里只有 3 条订单、1 条物流，趋势图与分布图完全不可读。
#      本脚本按「最近 N 天」生成一批时间分布真实的订单/物流数据，写成纯 INSERT + UPDATE 的 SQL，
#      供人工确认后手动执行。执行内容不含任何 DDL（不建表、不改结构、不动触发器）。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File tools\gen_seed_demo_stats.ps1
#   powershell -ExecutionPolicy Bypass -File tools\gen_seed_demo_stats.ps1 -Seed 7 -Days 30
#
# 输出：tools/seed_demo_stats.sql

param(
    [int]$Seed = 20241216,
    [int]$Days = 30
)

$ErrorActionPreference = "Stop"
$rand = [System.Random]::new($Seed)
$today = (Get-Date).Date

# 店铺：名称、单价
$shops = @(
    @{ Name = '大美鸡公煲';        Price = 15 },
    @{ Name = '天美自选饭';        Price = 10 },
    @{ Name = '天美馋嘴鸭';        Price = 16 },
    @{ Name = '小美烤盘饭';        Price = 12 },
    @{ Name = '小美章丘炒鸡';      Price = 16 },
    @{ Name = '小美负一楼蛋炒饭';  Price = 10 },
    @{ Name = '小美麻辣烫';        Price = 15 },
    @{ Name = '至美猪脚饭';        Price = 20 }
)

# 配送员：编号、姓名
$dispatchers = @(
    @{ Id = '010100'; Name = '摇摆羊' },
    @{ Id = '010101'; Name = '小亮' },
    @{ Id = '1000011'; Name = '老八' },
    @{ Id = '10111'; Name = '赵三金' }
)

# 订餐方式权重：网上订餐 40% / 人工订餐 60%
$ways = @('网上订餐', '网上订餐', '人工订餐', '人工订餐', '人工订餐')

# 顾客池（手机号 -> 姓名、楼栋）
$customers = @(
    @{ Phone = '18682412818'; Name = '蔡明轩'; Room = '15公寓' },
    @{ Phone = '18873190398'; Name = '杨楚璇'; Room = '8公寓' },
    @{ Phone = '13525188888'; Name = '老三';   Room = '10公寓' },
    @{ Phone = '15967789756'; Name = '马天宇'; Room = '7公寓' },
    @{ Phone = '13365789765'; Name = '吴方';   Room = '12公寓' },
    @{ Phone = '15878977898'; Name = '李知恩'; Room = '3公寓' }
)

# 配送时长文案池（deliver_time 是 varchar，库里既有值形如 '20分钟'）
$deliverMinutes = @(15, 18, 20, 22, 25, 28, 30, 35, 40)

# 每日订单量：最近 7 天明显升温（约 33/天），第 8-30 天约 5/天 —— 让 7 天 / 30 天切换都有观感
function Get-DayCount([int]$offset) {
    if ($offset -le 6) {
        return 30 + $rand.Next(0, 8)
    }
    return 4 + $rand.Next(0, 4)
}

$orderId = 4   # 现有订单为 1..3，从 4 起续编
$orders = New-Object System.Collections.ArrayList
$wuliu = New-Object System.Collections.ArrayList

for ($offset = $Days - 1; $offset -ge 0; $offset--) {
    $date = $today.AddDays(-$offset)
    $count = Get-DayCount $offset

    # 当天用餐高峰：11:00-13:00 与 17:00-19:00 双峰
    $minutes = New-Object System.Collections.ArrayList
    for ($i = 0; $i -lt $count; $i++) {
        if ($i % 2 -eq 0) {
            $m = (11 * 60) + $rand.Next(0, 120)
        }
        else {
            $m = (17 * 60) + $rand.Next(0, 120)
        }
        [void]$minutes.Add($m)
    }
    [void]$minutes.Sort()

    foreach ($m in $minutes) {
        $hm = '{0:00}:{1:00}:{2:00}' -f [int]($m / 60), ($m % 60), $rand.Next(0, 60)
        $createTime = '{0} {1}' -f $date.ToString('yyyy-MM-dd'), $hm

        $shop = $shops[$rand.Next(0, $shops.Count)]
        $customer = $customers[$rand.Next(0, $customers.Count)]
        $way = $ways[$rand.Next(0, $ways.Count)]
        $money = $shop.Price

        # 状态分布：未派发 18% / 派送中 22% / 已送达 60%
        #
        # 注意：这里只是"抽签"，最终 checked 由下方「状态校正」统一写入。
        # 原因是数据库触发器 wuliu_insert 会在插入 wuliu 记录时把订单 checked 强制置为 1，
        # 因此在本语句里直接写 2（已送达）会被触发器覆盖成 1，导致"已完成/派送中"分布失真。
        # 这里统一先写 0，等 wuliu 全部插完后再用一条确定性的 UPDATE 校正。
        $roll = $rand.Next(0, 100)
        $checked = 0

        [void]$orders.Add([pscustomobject]@{
                Id         = $orderId
                Shop       = $shop.Name
                Money      = $money
                Way        = $way
                Phone      = $customer.Phone
                Name       = $customer.Name
                Room       = $customer.Room
                Checked    = $checked
                CreateTime = $createTime
            })

        # 非"未派发"的订单需要一条 wuliu 记录；ended 决定它最终是"派送中"还是"已送达"
        if ($roll -ge 18) {
            $disp = $dispatchers[$rand.Next(0, $dispatchers.Count)]
            $mins = $deliverMinutes[$rand.Next(0, $deliverMinutes.Count)]
            $endedValue = 0
            if ($roll -ge 40) { $endedValue = 1 }

            [void]$wuliu.Add([pscustomobject]@{
                    OrderId = $orderId
                    Phone   = $customer.Phone
                    DispId  = $disp.Id
                    Time    = ('{0}分钟' -f $mins)
                    Ended   = $endedValue
                })
        }

        $orderId++
    }
}

# ---------------- 输出 SQL ----------------

$sb = New-Object System.Text.StringBuilder
$null = $sb.AppendLine('-- ============================================================================')
$null = $sb.AppendLine('-- 统计数据演示数据（仅 INSERT + UPDATE，无任何 DDL）')
$null = $sb.AppendLine('--')
$null = $sb.AppendLine("-- 由 tools/gen_seed_demo_stats.ps1 生成（Seed=$Seed, Days=$Days）")
$null = $sb.AppendLine("-- 生成时间基准：$($today.ToString('yyyy-MM-dd'))；订单时间分布在最近 $Days 天（含今天）")
$null = $sb.AppendLine('--')
$null = $sb.AppendLine('-- 执行顺序在本文件中是<b>有讲究的</b>，请勿调整：')
$null = $sb.AppendLine('--   1) 先插 oorder（checked 一律写 0）；')
$null = $sb.AppendLine('--   2) 再插 wuliu（数据库触发器 wuliu_insert 会把对应订单的 checked 强制置 1）；')
$null = $sb.AppendLine('--   3) 最后一段「状态校正」按 wuliu.ended 把 checked 修正为 1（在途）或 2（已送达）。')
$null = $sb.AppendLine('-- 若不按此顺序，触发器会把"已送达"的订单改成"派送中"，看板状态分布会严重失真。')
$null = $sb.AppendLine('--')
$null = $sb.AppendLine('-- 影响的行：')
$null = $sb.AppendLine("--   oorder      +$($orders.Count) 行（order_id 从 4 开始续编）")
$null = $sb.AppendLine("--   wuliu       +$($wuliu.Count) 行（非未派发的订单各一条）")
$null = $sb.AppendLine('--   orderway      count 重算为 oorder 实际行数（原本是 51/108 的历史遗留值）')
$null = $sb.AppendLine('--   fastfood_shop m_sale_v 重算为 oorder 实际行数（模拟触发器累积的效果）')
$null = $sb.AppendLine('--   user_msg    +1 行（CMX 缺少明细行，否则管理端账号进用户端会报错）')
$null = $sb.AppendLine('--')
$null = $sb.AppendLine('-- 执行方式：')
$null = $sb.AppendLine('--   docker exec -i csu_db-mysql-1 mysql -uroot -p123456 caimingxuan < tools/seed_demo_stats.sql')
$null = $sb.AppendLine('--   （若中文显示为问号，加 --default-character-set=utf8mb4）')
$null = $sb.AppendLine('--')
$null = $sb.AppendLine('-- 回滚方式（只删本脚本插入的数据，order_id 1..3 是 init.sql 自带的示例订单）：')
$null = $sb.AppendLine('--   DELETE FROM wuliu  WHERE order_id > 3;')
$null = $sb.AppendLine('--   DELETE FROM oorder WHERE order_id > 3;')
$null = $sb.AppendLine('--   随后执行本文件末尾的两条 UPDATE 以重算统计列，或从备份恢复。')
$null = $sb.AppendLine('-- ============================================================================')
$null = $sb.AppendLine('')
$null = $sb.AppendLine('SET NAMES utf8mb4;')
$null = $sb.AppendLine('')
$null = $sb.AppendLine('-- 记录本批演示数据的起始 order_id（init.sql 自带 1..3，本批从 4 起），')
$null = $sb.AppendLine('-- 供末尾的状态校正限定范围，避免误改手工/页面新下的订单。')
$null = $sb.AppendLine("SET @seed_min_order_id := $($orders[0].Id);")
$null = $sb.AppendLine('')

# ---------------- user_msg ----------------
$null = $sb.AppendLine('-- ---------------------------- user_msg：补齐 CMX 明细 ----------------------------')
$null = $sb.AppendLine('INSERT INTO `user_msg` (`id`, `real_name`, `sex`, `age`, `mail`, `phone`, `user_name`)')
$null = $sb.AppendLine("SELECT 2, '蔡明轩', '男', 21, '320836@qq.com', '18682412818', 'CMX'")
$null = $sb.AppendLine('WHERE NOT EXISTS (SELECT 1 FROM `user_msg` WHERE `id` = 2);')
$null = $sb.AppendLine('')

# ---------------- oorder ----------------
$null = $sb.AppendLine('-- ---------------------------- oorder：最近 ' + $Days + ' 天的订单 ----------------------------')
$null = $sb.AppendLine('INSERT INTO `oorder` (`order_id`, `shop_name`, `order_money`, `order_way`, `cons_phone`, `cons_name`, `cons_addre`, `checked`, `create_time`) VALUES')
$vals = foreach ($o in $orders) {
    "({0}, '{1}', {2}, '{3}', '{4}', '{5}', '{6}', {7}, '{8}')" -f `
        $o.Id, $o.Shop, $o.Money, $o.Way, $o.Phone, $o.Name, $o.Room, $o.Checked, $o.CreateTime
}
$null = $sb.AppendLine((($vals -join ",`r`n") + ';'))
$null = $sb.AppendLine('')

# ---------------- wuliu ----------------
$null = $sb.AppendLine('-- ---------------------------- wuliu：派送记录 ----------------------------')
$null = $sb.AppendLine('INSERT INTO `wuliu` (`order_id`, `cons_phone`, `disp_id`, `deliver_time`, `ended`) VALUES')
$vals = foreach ($w in $wuliu) {
    "({0}, '{1}', '{2}', '{3}', {4})" -f $w.OrderId, $w.Phone, $w.DispId, $w.Time, $w.Ended
}
$null = $sb.AppendLine((($vals -join ",`r`n") + ';'))
$null = $sb.AppendLine('')

# ---------------- 状态校正 ----------------
$null = $sb.AppendLine('-- ---------------------------- 状态校正（必须在 wuliu 插入之后） ----------------------------')
$null = $sb.AppendLine('-- 背景：触发器 wuliu_insert 会在插入物流记录时把订单 checked 强制置 1（"已派发"）。')
$null = $sb.AppendLine('-- 因此这里按 wuliu.ended 把演示数据的状态一次性校正为：')
$null = $sb.AppendLine('--   ended = 1（已送达） -> checked = 2')
$null = $sb.AppendLine('--   ended = 0（在途）   -> checked = 1')
$null = $sb.AppendLine('--   无物流记录          -> checked = 0（未派发，保持原值即可，这里也显式写一次）')
$null = $sb.AppendLine('UPDATE `oorder` o')
$null = $sb.AppendLine('SET o.`checked` = CASE')
$null = $sb.AppendLine('        WHEN EXISTS (SELECT 1 FROM `wuliu` w WHERE w.`order_id` = o.`order_id` AND w.`ended` = 1) THEN 2')
$null = $sb.AppendLine('        WHEN EXISTS (SELECT 1 FROM `wuliu` w WHERE w.`order_id` = o.`order_id` AND w.`ended` = 0) THEN 1')
$null = $sb.AppendLine('        ELSE 0')
$null = $sb.AppendLine('    END')
$null = $sb.AppendLine('WHERE o.`order_id` >= @seed_min_order_id;')
$null = $sb.AppendLine('')

# ---------------- 统计列重算 ----------------
$null = $sb.AppendLine('-- ---------------------------- 统计列重算（让聚合结果自洽） ----------------------------')
$null = $sb.AppendLine('-- orderway.count：以 oorder 实际行数为准（触发器只在增量时维护，无法修正历史遗留值）')
$null = $sb.AppendLine('UPDATE `orderway` ow')
$null = $sb.AppendLine('SET ow.`count` = (SELECT COUNT(*) FROM `oorder` o WHERE o.`order_way` = ow.`orderway_name`);')
$null = $sb.AppendLine('')
$null = $sb.AppendLine('-- fastfood_shop.m_sale_v：同上，等价于触发器 order_insert_sale 累积后的结果')
$null = $sb.AppendLine('UPDATE `fastfood_shop` fs')
$null = $sb.AppendLine('SET fs.`m_sale_v` = (SELECT COUNT(*) FROM `oorder` o WHERE o.`shop_name` = fs.`shop_name`);')
$null = $sb.AppendLine('')
$null = $sb.AppendLine('-- ---------------------------- 校验 ----------------------------')
$null = $sb.AppendLine('-- SELECT checked, COUNT(*), SUM(order_money) FROM oorder GROUP BY checked;')
$null = $sb.AppendLine('-- SELECT DATE(create_time), COUNT(*), SUM(order_money) FROM oorder GROUP BY DATE(create_time) ORDER BY 1;')
$null = $sb.AppendLine('-- SELECT d.dispatcher_name, COUNT(w.order_id) FROM dispatcher d LEFT JOIN wuliu w ON w.disp_id=d.dispatcher_id GROUP BY d.dispatcher_id;')

$outFile = Join-Path $PSScriptRoot 'seed_demo_stats.sql'
# MySQL 客户端按 utf8mb4 读取，写 UTF-8 无 BOM
[System.IO.File]::WriteAllText($outFile, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))

Write-Host "[seed] 订单 $($orders.Count) 条 / 物流 $($wuliu.Count) 条 -> $outFile" -ForegroundColor Green
Write-Host "[seed] 最早订单时间：$($orders[0].CreateTime)  最晚：$($orders[$orders.Count-1].CreateTime)"
