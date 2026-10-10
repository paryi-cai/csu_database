# 接口对照脚本：同一请求分别打 Flask（旧）与 Spring Boot（新），逐接口比对响应。
#
# 兼容 Windows PowerShell 5.1（不用 PS7 专有语法；HTTP 用 HttpWebRequest 以强制 UTF-8 解码，
# 因为 5.1 的 Invoke-WebRequest 会按 ISO-8859-1 解码响应体，中文会变乱码）。
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File tools\compare_api.ps1
#   powershell -ExecutionPolicy Bypass -File tools\compare_api.ps1 -FlaskBase http://127.0.0.1:5001 -JavaBase http://127.0.0.1:5000
#   powershell -ExecutionPolicy Bypass -File tools\compare_api.ps1 -SkipWrites   # 只比对只读接口
#
# 比对层级：
#   1) HTTP 状态码
#   2) 语义 JSON（解析后深比较，与字段顺序、unicode 转义大小写无关）
#   3) 规范化后的响应体原文（\uXXXX 十六进制统一小写、动态值占位符化）
#
# 已知差异（单独归类为 KNOWN-DIFF，不算失败）：
#   * 非 ASCII 转义大小写：Python 输出 \u767b，Jackson 输出 \u767B —— 语义等价
#   * token / create_time / order_id 为动态值，按占位符归一化后比较

[CmdletBinding()]
param(
    [string]$FlaskBase = "http://127.0.0.1:5000",
    [string]$JavaBase = "http://127.0.0.1:5001",
    [string]$AdminUser = "CMX",
    [string]$AdminPassword = "Aa.460734909",
    [string]$UserPhone = "18873190398",
    [string]$UserPassword = "aA.460734909",
    [switch]$SkipWrites,
    [string]$OutDir
)

$ErrorActionPreference = "Stop"
$script:results = New-Object System.Collections.ArrayList

if (-not $OutDir) {
    $OutDir = Join-Path (Split-Path -Parent $PSScriptRoot) "tools\compare-results"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"

function Write-Head($text) {
    Write-Host ""
    Write-Host "=== $text ===" -ForegroundColor Cyan
}

# 发送请求，返回 @{ Status; Body }；强制 UTF-8 解码
function Invoke-Api {
    param(
        [string]$Base,
        [string]$Method,
        [string]$Path,
        $Body,
        [string]$Token
    )
    if ([string]::IsNullOrEmpty($Base)) { return @{ Status = -1; Body = "NO BASE URL" } }

    $url = $Base.TrimEnd('/') + $Path
    try {
        $request = [System.Net.HttpWebRequest]::Create($url)
        $request.Method = $Method
        $request.Timeout = 20000
        $request.Accept = "application/json"
        if ($Token) { $request.Headers.Add("token", $Token) }

        if ($null -ne $Body) {
            $json = $Body
            if ($Body -isnot [string]) { $json = $Body | ConvertTo-Json -Compress -Depth 6 }
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
            $request.ContentType = "application/json; charset=utf-8"
            $request.ContentLength = $bytes.Length
            $stream = $request.GetRequestStream()
            $stream.Write($bytes, 0, $bytes.Length)
            $stream.Close()
        }

        try {
            $response = $request.GetResponse()
        } catch [System.Net.WebException] {
            $response = $_.Exception.Response
            if ($null -eq $response) {
                return @{ Status = -1; Body = "REQUEST FAILED: $($_.Exception.Message)" }
            }
        }

        $reader = New-Object System.IO.StreamReader($response.GetResponseStream(), [System.Text.Encoding]::UTF8)
        $text = $reader.ReadToEnd()
        $reader.Close()
        $status = [int]$response.StatusCode
        $response.Close()
        return @{ Status = $status; Body = $text }
    } catch {
        return @{ Status = -1; Body = "REQUEST FAILED: $($_.Exception.Message)" }
    }
}

# 规范化响应体：统一 unicode 转义大小写 + 动态值占位符化
function Normalize-Body {
    param([string]$Text)
    if ([string]::IsNullOrEmpty($Text)) { return "" }
    $t = $Text
    $t = [regex]::Replace($t, '\\u([0-9a-fA-F]{4})', { param($m) '\u' + $m.Groups[1].Value.ToLower() })
    $t = [regex]::Replace($t, '"token": "[^"]*"', '"token": "<TOKEN>"')
    $t = [regex]::Replace($t, '"create_time": "[^"]*"', '"create_time": "<TIME>"')
    $t = [regex]::Replace($t, '"order_id": \d+', '"order_id": <ID>')
    return $t.TrimEnd([char]13, [char]10)
}

function ConvertTo-Semantic {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    try { return $Text | ConvertFrom-Json } catch { return $null }
}

function Test-IsArray {
    param($Value)
    if ($null -eq $Value) { return $false }
    if ($Value -is [string]) { return $false }
    if ($Value -is [System.Management.Automation.PSCustomObject]) { return $false }
    return ($Value -is [System.Collections.IEnumerable])
}

# 深比较，返回差异描述列表
function Compare-Semantic {
    param($Left, $Right, [string]$Path = '$')
    $diffs = New-Object System.Collections.ArrayList

    if ($null -eq $Left -and $null -eq $Right) { return $diffs }
    if ($null -eq $Left -or $null -eq $Right) {
        [void]$diffs.Add("$Path : 一侧为 null (flask=$Left, java=$Right)")
        return $diffs
    }

    if ((Test-IsArray $Left) -or (Test-IsArray $Right)) {
        $la = @($Left); $ra = @($Right)
        if ($la.Count -ne $ra.Count) {
            [void]$diffs.Add("$Path : 数组长度不同 (flask=$($la.Count), java=$($ra.Count))")
            return $diffs
        }
        for ($i = 0; $i -lt $la.Count; $i++) {
            $sub = Compare-Semantic $la[$i] $ra[$i] "$Path[$i]"
            if ($null -ne $sub) { [void]$diffs.AddRange(@($sub)) }
        }
        return $diffs
    }

    $leftIsObj = $Left -is [System.Management.Automation.PSCustomObject]
    $rightIsObj = $Right -is [System.Management.Automation.PSCustomObject]
    if ($leftIsObj -or $rightIsObj) {
        if (-not ($leftIsObj -and $rightIsObj)) {
            [void]$diffs.Add("$Path : 一侧为对象另一侧为值 (flask=$Left, java=$Right)")
            return $diffs
        }
        $lp = @($Left.PSObject.Properties | ForEach-Object { $_.Name })
        $rp = @($Right.PSObject.Properties | ForEach-Object { $_.Name })
        foreach ($k in $lp) { if ($rp -notcontains $k) { [void]$diffs.Add("$Path.$k : 仅 Flask 有该字段") } }
        foreach ($k in $rp) { if ($lp -notcontains $k) { [void]$diffs.Add("$Path.$k : 仅 Java 有该字段") } }
        foreach ($k in $lp) {
            if ($rp -notcontains $k) { continue }
            if ($k -eq "token" -or $k -eq "create_time") { continue }
            $sub = Compare-Semantic $Left.$k $Right.$k "$Path.$k"
            if ($null -ne $sub) { [void]$diffs.AddRange(@($sub)) }
        }
        return $diffs
    }

    if ("$Left" -ne "$Right") {
        [void]$diffs.Add("$Path : flask=<$Left> java=<$Right>")
    }
    return $diffs
}

function Add-Result {
    param([string]$Name, [string]$Verdict, [string]$Detail, $Flask, $Java)
    [void]$script:results.Add([PSCustomObject]@{
            Api     = $Name
            Verdict = $Verdict
            Detail  = $Detail
            Flask   = $(if ($Flask) { $Flask.Body } else { "" })
            Java    = $(if ($Java) { $Java.Body } else { "" })
        })
    $color = "Red"
    if ($Verdict -eq "MATCH") { $color = "Green" }
    elseif ($Verdict -eq "KNOWN-DIFF") { $color = "Yellow" }
    elseif ($Verdict -eq "SKIP") { $color = "DarkGray" }
    Write-Host ("  [{0,-10}] {1}" -f $Verdict, $Name) -ForegroundColor $color
    if ($Verdict -ne "MATCH" -and $Verdict -ne "SKIP") {
        Write-Host "               $Detail" -ForegroundColor $color
    }
}

function Compare-One {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        $Body,
        [string]$FlaskToken,
        [string]$JavaToken
    )
    $flask = Invoke-Api -Base $FlaskBase -Method $Method -Path $Path -Body $Body -Token $FlaskToken
    $java = Invoke-Api -Base $JavaBase -Method $Method -Path $Path -Body $Body -Token $JavaToken

    if ($java.Status -eq -1) { Add-Result $Name "JAVA-FAIL" $java.Body $flask $java; return }
    if ($flask.Status -eq -1) { Add-Result $Name "FLASK-FAIL" $flask.Body $flask $java; return }

    $statusMatch = ($flask.Status -eq $java.Status)
    $semDiffs = Compare-Semantic (ConvertTo-Semantic $flask.Body) (ConvertTo-Semantic $java.Body)
    $flaskNorm = Normalize-Body $flask.Body
    $javaNorm = Normalize-Body $java.Body

    if ($statusMatch -and $semDiffs.Count -eq 0 -and $flaskNorm -eq $javaNorm) {
        Add-Result $Name "MATCH" "" $flask $java
    } elseif ($statusMatch -and $semDiffs.Count -eq 0) {
        Add-Result $Name "KNOWN-DIFF" "语义一致，仅字节排版不同（unicode 转义大小写）" $flask $java
    } else {
        $detail = ""
        if (-not $statusMatch) {
            $detail = "HTTP 状态码不同: Flask=$($flask.Status) Java=$($java.Status)"
        } else {
            $detail = (($semDiffs | Select-Object -First 4) -join " ; ")
        }
        Add-Result $Name "DIFF" $detail $flask $java
    }
}

function Get-TokenFromBody {
    param([string]$Body)
    if ([string]::IsNullOrEmpty($Body)) { return $null }
    $m = [regex]::Match($Body, '"token":\s*"([^"]+)"')
    if ($m.Success) { return $m.Groups[1].Value }
    return $null
}

# ============================ 开始 ============================
Write-Host "Flask (旧): $FlaskBase" -ForegroundColor White
Write-Host "Java  (新): $JavaBase" -ForegroundColor White
if ($SkipWrites) { Write-Host "模式: 仅只读接口" -ForegroundColor White }
else { Write-Host "模式: 只读 + 写接口" -ForegroundColor White }

Write-Head "0. 登录（获取 token）"
$loginBodyUser = @{ userortel = $UserPhone; password = $UserPassword }
$flaskLogin = Invoke-Api -Base $FlaskBase -Method "POST" -Path "/api/user/login" -Body $loginBodyUser
$javaLogin = Invoke-Api -Base $JavaBase -Method "POST" -Path "/api/user/login" -Body $loginBodyUser

Compare-One "POST /api/user/login（普通用户）" "POST" "/api/user/login" $loginBodyUser $null $null
Compare-One "POST /api/user/login（管理员）" "POST" "/api/user/login" `
    (@{ userortel = $AdminUser; password = $AdminPassword }) $null $null
Compare-One "POST /api/user/login（密码错误）" "POST" "/api/user/login" `
    (@{ userortel = $UserPhone; password = "wrong-password" }) $null $null

$flaskToken = Get-TokenFromBody $flaskLogin.Body
$javaToken = Get-TokenFromBody $javaLogin.Body
if ($flaskToken) { Write-Host "  Flask token 已获取" -ForegroundColor DarkGray }
else { Write-Host "  [WARN] 未获取到 Flask token，受保护接口会返回 401（请确认 Flask 端已启动且账号存在）" -ForegroundColor Yellow }
if ($javaToken) { Write-Host "  Java  token 已获取" -ForegroundColor DarkGray }
else { Write-Host "  [WARN] 未获取到 Java token" -ForegroundColor Yellow }

Write-Head "1. 用户端 —— 只读接口"
Compare-One "GET /api/user/shop" "GET" "/api/user/shop" $null $null $null
Compare-One "GET /api/user/unsend" "GET" "/api/user/unsend" $null $flaskToken $javaToken
Compare-One "GET /api/user/sending" "GET" "/api/user/sending" $null $flaskToken $javaToken
Compare-One "GET /api/user/sended" "GET" "/api/user/sended" $null $flaskToken $javaToken
Compare-One "GET /api/user/usermsg" "GET" "/api/user/usermsg" $null $flaskToken $javaToken
Compare-One "GET /api/user/unsend（无 token，应 401）" "GET" "/api/user/unsend" $null $null $null
Compare-One "GET /api/user/unsend（坏 token，应 401）" "GET" "/api/user/unsend" $null "not-a-jwt" "not-a-jwt"

Write-Head "2. 管理端 —— 只读接口"
Compare-One "GET /api/manager/shop" "GET" "/api/manager/shop" $null $null $null
Compare-One "GET /api/manager/server" "GET" "/api/manager/server" $null $null $null
Compare-One "GET /api/manager/dispatcher" "GET" "/api/manager/dispatcher" $null $null $null
Compare-One "GET /api/manager/unsend" "GET" "/api/manager/unsend" $null $null $null
Compare-One "GET /api/manager/sending" "GET" "/api/manager/sending" $null $null $null
Compare-One "GET /api/manager/sended" "GET" "/api/manager/sended" $null $null $null
Compare-One "GET /api/manager/wuliu?id=0" "GET" "/api/manager/wuliu?id=0" $null $null $null
Compare-One "GET /api/manager/wuliu?id=1" "GET" "/api/manager/wuliu?id=1" $null $null $null
Compare-One "GET /api/manager/wuliu（缺省 id）" "GET" "/api/manager/wuliu" $null $null $null

if (-not $SkipWrites) {
    Write-Head "3. 写接口（两个后端各自新增/删除自己的数据；用例设计为可重复执行）"

    $shopName = "对照测试店铺"
    $serviceId = "990001"
    $dispatcherId = "990001"

    Compare-One "POST /api/manager/shop（action=add）" "POST" "/api/manager/shop" `
        (@{ action = "add"; shop_name = $shopName; price = "18"; m_sale_v = "66" }) $null $null
    Compare-One "POST /api/manager/shop（重复 add）" "POST" "/api/manager/shop" `
        (@{ action = "add"; shop_name = $shopName; price = "18"; m_sale_v = "66" }) $null $null
    Compare-One "POST /api/manager/shop（action=change）" "POST" "/api/manager/shop" `
        (@{ action = "change"; shop_name = $shopName; price = "20"; m_sale_v = "77" }) $null $null
    Compare-One "DELETE /api/manager/shop" "DELETE" "/api/manager/shop" `
        (@{ want_delete = $shopName }) $null $null
    Compare-One "DELETE /api/manager/shop（重复删除）" "DELETE" "/api/manager/shop" `
        (@{ want_delete = $shopName }) $null $null

    Compare-One "POST /api/manager/server（新增）" "POST" "/api/manager/server" `
        (@{ service_id = $serviceId; service_name = "对照测试服务员"; fastfood_shop_name = "大美鸡公煲" }) $null $null
    Compare-One "POST /api/manager/server（重复）" "POST" "/api/manager/server" `
        (@{ service_id = $serviceId; service_name = "对照测试服务员"; fastfood_shop_name = "大美鸡公煲" }) $null $null
    Compare-One "DELETE /api/manager/server" "DELETE" "/api/manager/server" `
        (@{ want_delete = $serviceId }) $null $null

    Compare-One "POST /api/manager/dispatcher（新增）" "POST" "/api/manager/dispatcher" `
        (@{ dispatcher_id = $dispatcherId; dispatcher_name = "对照测试配送员"; dispatcher_phone = "13900000000" }) $null $null
    Compare-One "POST /api/manager/dispatcher（重复）" "POST" "/api/manager/dispatcher" `
        (@{ dispatcher_id = $dispatcherId; dispatcher_name = "对照测试配送员"; dispatcher_phone = "13900000000" }) $null $null
    Compare-One "DELETE /api/manager/dispatcher" "DELETE" "/api/manager/dispatcher" `
        (@{ want_delete = $dispatcherId }) $null $null

    Write-Host "  派发订单（各后端使用自己的未派发订单号，因此只比对结构与文案）：" -ForegroundColor DarkGray
    Compare-One "POST /api/manager/unsend（派发订单 1）" "POST" "/api/manager/unsend" `
        (@{ order_id = 1; dispatcher_id = "010100"; deliver_time = "30分钟" }) $null $null

    Write-Host "  以下接口会改变数据且涉及自增主键，无法逐字节比对，请按 README 手工核对：" -ForegroundColor DarkGray
    Write-Host "    POST /api/user/addorder、POST /api/user/unsend（改单）、DELETE /api/user/unsend（取消）、POST /api/user/pwd_chg" -ForegroundColor DarkGray
}

# ============================ 汇总 ============================
Write-Head "汇总"
$total = $script:results.Count
$match = @($script:results | Where-Object { $_.Verdict -eq "MATCH" }).Count
$known = @($script:results | Where-Object { $_.Verdict -eq "KNOWN-DIFF" }).Count
$diff = @($script:results | Where-Object { $_.Verdict -eq "DIFF" }).Count
$fail = @($script:results | Where-Object { $_.Verdict -like "*FAIL*" }).Count

Write-Host ("  总数 {0}：完全一致 {1}，语义一致(仅排版差异) {2}，有差异 {3}，请求失败 {4}" -f `
        $total, $match, $known, $diff, $fail) -ForegroundColor White

$json = Join-Path $OutDir "compare-$stamp.json"
$script:results | ConvertTo-Json -Depth 8 | Set-Content -Path $json -Encoding UTF8
Write-Host "  详细结果：$json" -ForegroundColor DarkGray

if ($diff -gt 0 -or $fail -gt 0) {
    Write-Host ""
    Write-Host "  有差异的接口：" -ForegroundColor Red
    $script:results | Where-Object { $_.Verdict -eq "DIFF" -or $_.Verdict -like "*FAIL*" } | ForEach-Object {
        Write-Host ("    - {0}: {1}" -f $_.Api, $_.Detail) -ForegroundColor Red
    }
    exit 1
}

Write-Host ""
Write-Host "  所有接口契约一致。" -ForegroundColor Green
exit 0
