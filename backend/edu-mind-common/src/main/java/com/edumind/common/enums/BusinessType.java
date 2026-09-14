package com.edumind.common.enums;

import lombok.Getter;

/**
 * 业务操作类型枚举（对齐 E:\wu-admin，并扩充教育业务）
 */
@Getter
public enum BusinessType {

    /**
     * 其它
     */
    OTHER(0, "其它"),

    /**
     * 新增
     */
    INSERT(1, "新增"),

    /**
     * 修改
     */
    UPDATE(2, "修改"),

    /**
     * 删除
     */
    DELETE(3, "删除"),

    /**
     * 查询
     */
    QUERY(4, "查询"),

    /**
     * 导出
     */
    EXPORT(5, "导出"),

    /**
     * 导入
     */
    IMPORT(6, "导入"),

    /**
     * 授权/状态变更/分配
     */
    GRANT(7, "授权/变更"),

    /**
     * 清空
     */
    CLEAN(8, "清空");

    private final int value;
    private final String description;

    BusinessType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static BusinessType of(Integer value) {
        if (value == null) {
            return OTHER;
        }
        for (BusinessType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return OTHER;
    }
}
