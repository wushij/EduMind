package com.edumind.knowledge.support;

/**
 * 课程课件是否可进入知识库解析流水线的类型判断（供 resource 等模块复用，避免依赖 knowledge 模块实现）。
 */
public final class CourseResourceKnowledgeSyncSupport {

    private CourseResourceKnowledgeSyncSupport() {
    }

    public static boolean isSyncableType(String resourceType, String fileName) {
        String type = resourceType != null ? resourceType.toUpperCase() : "";
        String name = fileName != null ? fileName.toLowerCase() : "";
        if ("MD".equals(type) || "TXT".equals(type) || "PDF".equals(type) || "WORD".equals(type)
                || "DOCUMENT".equals(type)) {
            return true;
        }
        return name.endsWith(".md") || name.endsWith(".markdown") || name.endsWith(".txt")
                || name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx");
    }

    public static String normalizeFileType(String resourceType, String fileName) {
        if (resourceType != null && !resourceType.isBlank()) {
            return resourceType.toUpperCase();
        }
        String lower = fileName != null ? fileName.toLowerCase() : "";
        if (lower.endsWith(".pdf")) {
            return "PDF";
        }
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
            return "WORD";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "MD";
        }
        if (lower.endsWith(".txt")) {
            return "TXT";
        }
        return "DOCUMENT";
    }
}
