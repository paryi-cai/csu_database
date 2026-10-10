#!/bin/bash
# 验证 docker/wait-for-mysql.sh 的探测逻辑：连上之后是否【立即】返回。
# 用法：bash verify-wait-probe.sh <host> <port> <期望秒数上限>
set -u

HOST="${1:-127.0.0.1}"
PORT="${2:-3999}"
LIMIT_MS="${3:-5000}"

echo "--- 探测 ${HOST}:${PORT} ---"

# 复刻 wait-for-mysql.sh 的 probe() 逻辑
start_ms=$(date +%s%3N 2>/dev/null || echo 0)
if command -v timeout >/dev/null 2>&1; then
    echo "  使用 timeout $(command -v timeout)"
    timeout 2 bash -c "exec 3<>/dev/tcp/${HOST}/${PORT}" 2>/dev/null
    rc=$?
else
    echo "  无 timeout，直接探测"
    bash -c "exec 3<>/dev/tcp/${HOST}/${PORT}" 2>/dev/null
    rc=$?
fi
end_ms=$(date +%s%3N 2>/dev/null || echo 0)

if [ "$rc" -eq 0 ]; then
    elapsed=$((end_ms - start_ms))
    echo "  结果：连接成功 (rc=$rc)，耗时 ${elapsed} ms"
    if [ "$elapsed" -lt "$LIMIT_MS" ]; then
        echo "  判定：PASS —— 连上即返回（未等待对端关闭）"
    else
        echo "  判定：FAIL —— 耗时超过 ${LIMIT_MS} ms，说明在等待对端"
    fi
else
    echo "  结果：连接失败 (rc=$rc)"
    echo "  判定：PASS —— 未连上时正常返回非 0"
fi
