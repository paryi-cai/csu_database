package com.csu.takeout.config;

import com.csu.takeout.interceptor.JwtTokenInterceptor;
import com.csu.takeout.json.UnifiedJsonMessageConverter;
import com.csu.takeout.json.UnifiedJsonObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 层配置类，注册拦截器、跨域、静态资源与接口文档。
 *
 * <p>组织方式对齐 sky-take-out 的 WebMvcConfiguration，差异点：</p>
 * <ul>
 *   <li>拦截器只注册一个（既有接口只有一套 token），且只拦截 既有接口真正做校验的 6 个接口；</li>
 *   <li>新增 {@link CorsFilter}：既有接口每个路由都带 {@code @cross_origin()}，前端 8080 端口
 *       跨域访问 5000 端口，必须保留跨域支持；</li>
 *   <li>接口文档由 springfox 的 Docket 换为 springdoc 的 GroupedOpenApi（Knife4j 4.x）；</li>
 *   <li>消息转换器使用 统一的 {@link UnifiedJsonObjectMapper}。</li>
 * </ul>
 *
 * <p><b>为什么实现 {@link WebMvcConfigurer} 而不是继承 {@code WebMvcConfigurationSupport}：</b>
 * 继承后者会通过 {@code @ConditionalOnMissingBean(WebMvcConfigurationSupport.class)}
 * <b>关闭 Spring Boot 的 Web MVC 自动配置</b>，导致 {@code spring.mvc.*} 配置项、
 * 自动注册的消息转换器、静态资源默认处理等全部失效。本项目当前功能虽能自洽，
 * 但为了保留 Boot 的默认行为（例如将来要设置 {@code spring.mvc.servlet.path}、
 * 或让 springdoc 的默认路径映射正常工作），改为实现接口更稳妥。</p>
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtTokenInterceptor jwtTokenInterceptor;

    /**
     * 需要 JWT 校验的接口（需要登录态的用户端接口）。
     * 登录、店铺列表以及<b>既有</b>管理端接口没有做令牌校验，此处保持一致。
     *
     * <p>数据统计（/api/manager/stats/**）与异常告警（/api/manager/alert/**）是本项目
     * 新增的功能，不属于"既有接口"，因此按新增功能的要求纳入校验：只有携带合法 token
     * 的请求才能访问。前端所有请求都会自动带上 token，因此对现有页面零影响；
     * 越权访问返回 401 + {@code {"code":1000,"msg":"用户未登录"}}。</p>
     */
    private static final String[] JWT_PATH_PATTERNS = {
            "/api/user/addorder",
            "/api/user/unsend",
            "/api/user/sending",
            "/api/user/sended",
            "/api/user/usermsg",
            "/api/user/usermsg/**",
            "/api/user/pwd_chg",
            "/api/manager/stats/**",
            "/api/manager/alert/**"
    };

    /**
     * 注册自定义拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenInterceptor).addPathPatterns(JWT_PATH_PATTERNS);
    }

    /**
     * 跨域配置：允许前端（localhost:8080）直连本服务
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 回显请求来源（等价于 跨域默认行为），同时允许无 Origin 的直连调用
        config.setAllowedOriginPatterns(List.of("*"));
        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addAllowedMethod(CorsConfiguration.ALL);
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 接口文档：用户端
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("1-用户端接口")
                .packagesToScan("com.csu.takeout.controller.user")
                .build();
    }

    /**
     * 接口文档：管理端
     */
    @Bean
    public GroupedOpenApi managerApi() {
        return GroupedOpenApi.builder()
                .group("2-管理端接口")
                .packagesToScan("com.csu.takeout.controller.manager")
                .build();
    }

    /**
     * 接口文档基本信息
     */
    @Bean
    public OpenAPI csuTakeoutOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("校园快餐订餐管理系统接口文档")
                .version("1.0")
                .description("校园快餐订餐管理系统后端，响应结构与前端契约完全兼容"));
    }

    /**
     * 设置静态资源映射（Knife4j / Swagger UI）。
     *
     * <p>{@code classpath:/META-INF/resources/} 本身就在 Boot 的默认静态资源位置中
     * （{@code spring.web.resources.static-locations} 已包含它），这里显式注册是为了
     * 不依赖该默认值，保证 /doc.html 一定能访问。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/META-INF/resources/");
    }

    /**
     * 拓展消息转化器：使用 统一 JSON 序列化器，保证响应体逐字节一致
     * （含空格排版、末尾换行、非 ASCII 转义）
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new UnifiedJsonMessageConverter(unifiedObjectMapper()));
    }

    /**
     * 统一响应格式的对象映射器
     */
    @Bean
    public ObjectMapper unifiedObjectMapper() {
        return new UnifiedJsonObjectMapper();
    }
}
