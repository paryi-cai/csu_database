package com.csu.takeout;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 校园快餐订餐管理系统 —— Spring Boot 3 启动类
 *
 * <p>保持接口路径、
 * 请求参数名与响应 JSON 结构与契约完全一致，原 Vue 前端无需任何改动。</p>
 */
@SpringBootApplication
@EnableTransactionManagement // 开启注解方式的事务管理
@MapperScan("com.csu.takeout.mapper")
@Slf4j
public class CsuTakeoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsuTakeoutApplication.class, args);
        log.info("csu-takeout server started, API: http://localhost:5000  docs: http://localhost:5000/doc.html");
    }
}
