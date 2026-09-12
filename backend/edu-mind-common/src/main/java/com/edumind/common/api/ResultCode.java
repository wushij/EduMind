package com.edumind.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "系统繁忙，请稍后重试"),
    VALIDATE_FAILED(400, "参数检验失败"),
    UNAUTHORIZED(401, "暂未登录或Token已经过期"),
    FORBIDDEN(403, "没有相关权限"),
    RESOURCE_NOT_FOUND(404, "请求资源未找到"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试");

    private final Integer code;
    private final String message;
}