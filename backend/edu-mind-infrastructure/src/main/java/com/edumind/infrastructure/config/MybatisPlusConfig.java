package com.edumind.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.edumind.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus 全局插件配置 (包含分页插件与统一多租户行级数据隔离拦截器)
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 已纳入多租户物理隔离的业务表清单 (严格在 SQL 解析层追加 WHERE tenant_id = ?)
     */
    private static final Set<String> TENANT_TABLES = Set.of(
            "sys_campus",
            "sys_organization",
            "sys_term",
            "sys_tenant_member",
            "sys_member_org",
            "sys_tenant_quota",
            "ai_memory_namespace",
            "knowledge_ocr_task",
            "export_task",
            "teaching_intervention",
            "course",
            "knowledge_base",
            "ai_conversation",
            "ai_call_log",
            "edu_question",
            "sys_notification",
            "sys_notification_broadcast",
            "security_key_version"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 租户隔离行级拦截器 (必须置于分页插件之前执行)
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.getTenantId();
                if (tenantId == null) {
                    // 若缺失租户，采用安全负数隔离，避免泄漏任何正式租户数据
                    return new LongValue(-1L);
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 若处于平台运维/显式忽略租户上下文中，则全量免除隔离
                if (TenantContext.isIgnoreTenant()) {
                    return true;
                }
                if (tableName == null || tableName.isBlank()) {
                    return true;
                }
                String cleanName = tableName.replace("`", "").replace("\"", "").replace("'", "").trim().toLowerCase();
                int dotIdx = cleanName.lastIndexOf('.');
                if (dotIdx >= 0) {
                    cleanName = cleanName.substring(dotIdx + 1);
                }
                return !TENANT_TABLES.contains(cleanName);
            }
        }));

        // 2. 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}