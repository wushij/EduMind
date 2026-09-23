package com.edumind.knowledge.support;

/**
 * 内部流水线调用标记。
 *
 * <p>文档流水线（解析 → 切片 → 向量化）在异步线程中执行，线程内没有登录用户上下文，
 * 而 {@code KnowledgeAccessService#assertCourseAccessible} 是面向「用户请求」的权限断言，
 * 在没有登录态时会直接抛「未登录」，导致切片与向量化被整条静默跳过
 * （表现为：文档 parse_status=SUCCESS、正文已入库，但切片数为 0、无索引任务）。</p>
 *
 * <p>安全边界：标记只能由服务端内部代码设置，不接收任何外部请求参数；
 * 内部调用方必须在进入前已完成用户鉴权（如上传/同步接口本身已 assertCanEdit）。</p>
 */
public final class InternalInvocationContext {

    private static final ThreadLocal<Boolean> INTERNAL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private InternalInvocationContext() {
    }

    public static boolean isInternal() {
        return Boolean.TRUE.equals(INTERNAL.get());
    }

    /**
     * 以内部调用身份执行，结束后恢复原有标记（支持嵌套调用），避免线程池复用造成标记残留。
     */
    public static void runInternal(Runnable action) {
        boolean previous = isInternal();
        INTERNAL.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            if (previous) {
                INTERNAL.set(Boolean.TRUE);
            } else {
                INTERNAL.remove();
            }
        }
    }
}
