# 校园快餐订餐管理系统 —— Java 后端镜像（多阶段构建）
#
# 构建：docker build -t csu-takeout-java .
# 运行：由 docker-compose.yml 编排（MySQL 3307 / Redis 6379 / API 5000）
#
# 说明：构建阶段在容器内执行 mvn package（跳过测试，测试依赖 H2，与生产无关），
#       运行阶段只保留 JRE 与可执行 jar，镜像体积更小。

# ---------- 构建阶段 ----------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先只复制 pom.xml，利用 Docker 层缓存避免改动源码时重复下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码并打包
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-jammy

# 时区与字符集：数据库中的 create_time 为 Asia/Shanghai 的本地时间字符串
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

# curl 用于 HEALTHCHECK（镜像本身不自带）
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# 启动前等待 MySQL 就绪的脚本（纯 shell，无需额外安装客户端）
COPY docker/wait-for-mysql.sh /app/wait-for-mysql.sh
RUN chmod +x /app/wait-for-mysql.sh

COPY --from=builder /build/target/csu-takeout.jar /app/app.jar

EXPOSE 5000

# 探活：Knife4j / OpenAPI 文档端点，能返回 200 说明应用与 Web 层均已就绪
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:5000/v3/api-docs/swagger-config > /dev/null || exit 1

ENTRYPOINT ["/app/wait-for-mysql.sh"]
