#!/bin/bash
# 等待 MySQL 就绪后再启动后端应用。
#
# 职责：等 MySQL 端口可连接后再拉起应用，避免容器编排下的启动竞态。
#
# ── 为什么必须是 "#!/bin/bash" 而不是 "#!/bin/sh" ──
#   eclipse-temurin 的 jammy 镜像里 /bin/sh 指向 dash，而 /dev/tcp 是 bash 专属特性。
#   写成 sh + /dev/tcp 会让探测永远失败，只能靠超时兜底，并且还会在超时后误报 ready。
#
# ── 为什么用 /dev/tcp 而不是 curl ──
#   曾用过 `curl telnet://host:port`，实测发现它的语义不是"连上即返回"：
#     · 端口拒绝：约 2s 退出（符合预期）
#     · 连上但对端不发数据、不关闭：会一直挂着，直到对端关闭或 curl 自身超时
#     · 连上且对端立即发数据（如 MySQL 握手包）：同样不会立即退出
#   实际后果：连上 MySQL 后仍要白等到 MySQL 按 connect_timeout 踢掉连接（默认约 10s），
#   每次启动多等约 10 秒；若遇到"接受 TCP 但不关闭"的防火墙/代理，还会一直卡住。
#   而 `exec 3<>/dev/tcp/host/port` 在 TCP 建连成功时立即返回 0，正是我们需要的行为。
#
# ── 为什么要套 timeout ──
#   /dev/tcp 在"被丢包/半开"等情况下可能长时间无返回，用 timeout 给单次探测加个上限，
#   保证整体循环始终受 WAIT_SECONDS 约束。timeout 来自 coreutils，Ubuntu 基础镜像自带；
#   若环境里没有 timeout，则退化为直接探测（循环本身仍有 WAIT_SECONDS 上限）。
set -u

HOST="${DB_HOST:-mysql}"
PORT="${DB_PORT:-3306}"
WAIT_SECONDS="${WAIT_SECONDS:-120}"
PROBE_TIMEOUT="${PROBE_TIMEOUT:-2}"

echo "[wait-for-mysql] waiting for ${HOST}:${PORT} (timeout ${WAIT_SECONDS}s) ..."

# 探测函数：连上立即返回 0，未连上返回非 0
probe() {
    if command -v timeout >/dev/null 2>&1; then
        timeout "${PROBE_TIMEOUT}" bash -c "exec 3<>/dev/tcp/${HOST}/${PORT}" 2>/dev/null
    else
        bash -c "exec 3<>/dev/tcp/${HOST}/${PORT}" 2>/dev/null
    fi
}

elapsed=0
ready=0
while [ "${elapsed}" -lt "${WAIT_SECONDS}" ]; do
    if probe; then
        ready=1
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

if [ "${ready}" -eq 1 ]; then
    echo "[wait-for-mysql] MySQL is ready (waited ${elapsed}s), starting backend ..."
else
    echo "[wait-for-mysql] WARNING: ${HOST}:${PORT} not reachable after ${WAIT_SECONDS}s, starting anyway"
fi

# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -jar /app/app.jar
