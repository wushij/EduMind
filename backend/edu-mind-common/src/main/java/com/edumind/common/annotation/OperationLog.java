package com.edumind.common.annotation;

import com.edumind.common.enums.BusinessType;

import java.lang.annotation.*;

/**
 * 系统操作日志记录注解（对标 E:\wu-admin @Log 注解，并在全平台规范命名为 @OperationLog）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块名称（如：用户管理、课程管理、系统配置）
     */
    String module() default "";

    /**
     * 功能标题/操作名称（如：重置密码、修改配置、新增题目）
     */
    String title() default "";

    /**
     * 业务操作类型
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 是否保存请求入参
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应出参
     */
    boolean isSaveResponseData() default true;
}
