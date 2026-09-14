package com.edumind.common.utils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 业务实体差异比对工具（深度对齐并升级 E:\wu-admin OperLogDiffUtils）
 * 通过反射对比操作前后对象的属性变化，生成人类可读的中文变更明细。
 */
public class OperationLogDiffUtils {

    private static final Map<String, String> FIELD_NAMES = new HashMap<>();

    static {
        // 用户与组织
        FIELD_NAMES.put("username", "用户名");
        FIELD_NAMES.put("realName", "真实姓名");
        FIELD_NAMES.put("nickname", "用户昵称");
        FIELD_NAMES.put("email", "邮箱地址");
        FIELD_NAMES.put("phone", "手机号码");
        FIELD_NAMES.put("mobile", "手机号码");
        FIELD_NAMES.put("status", "状态");
        FIELD_NAMES.put("department", "所属院系");
        FIELD_NAMES.put("roleIds", "角色列表");
        FIELD_NAMES.put("roleCode", "角色标识");
        FIELD_NAMES.put("roleName", "角色名称");
        FIELD_NAMES.put("permissionCodes", "权限列表");

        // 租户与校区
        FIELD_NAMES.put("tenantName", "租户名称");
        FIELD_NAMES.put("tenantCode", "租户编码");
        FIELD_NAMES.put("planCode", "套餐版本");
        FIELD_NAMES.put("expireTime", "过期时间");
        FIELD_NAMES.put("campusName", "校区名称");
        FIELD_NAMES.put("orgName", "组织名称");

        // 课程与教学
        FIELD_NAMES.put("courseName", "课程名称");
        FIELD_NAMES.put("courseCode", "课程代码");
        FIELD_NAMES.put("term", "开课学期");
        FIELD_NAMES.put("title", "标题");
        FIELD_NAMES.put("description", "描述");
        FIELD_NAMES.put("score", "分值");
        FIELD_NAMES.put("difficulty", "难度等级");
        FIELD_NAMES.put("durationMinutes", "考试时长(分钟)");

        // 知识库与系统参数
        FIELD_NAMES.put("kbName", "知识库名称");
        FIELD_NAMES.put("configKey", "配置键");
        FIELD_NAMES.put("configValue", "配置值");
        FIELD_NAMES.put("configName", "配置名称");
        FIELD_NAMES.put("remark", "备注说明");
        FIELD_NAMES.put("sort", "排序号");
    }

    public static List<String> diff(Object oldObj, Object newObj) {
        List<String> diffs = new ArrayList<>();
        if (oldObj == null || newObj == null) {
            return diffs;
        }
        Class<?> newClazz = newObj.getClass();
        Class<?> oldClazz = oldObj.getClass();

        for (Field newField : newClazz.getDeclaredFields()) {
            String fieldName = newField.getName();
            if ("id".equals(fieldName) || "serialVersionUID".equals(fieldName)
                    || "createTime".equals(fieldName) || "updateTime".equals(fieldName)
                    || "password".equals(fieldName) || "salt".equals(fieldName)) {
                continue;
            }
            String chineseName = FIELD_NAMES.getOrDefault(fieldName, fieldName);
            try {
                newField.setAccessible(true);
                Object newVal = newField.get(newObj);

                Field oldField = findField(oldClazz, fieldName);
                if (oldField != null) {
                    oldField.setAccessible(true);
                    Object oldVal = oldField.get(oldObj);

                    if (!Objects.equals(oldVal, newVal)) {
                        if (oldVal == null && (newVal instanceof String && ((String) newVal).isEmpty())) {
                            continue;
                        }
                        if (newVal == null && (oldVal instanceof String && ((String) oldVal).isEmpty())) {
                            continue;
                        }

                        String oldStr = formatVal(fieldName, oldVal);
                        String newStr = formatVal(fieldName, newVal);
                        diffs.add(chineseName + ": " + oldStr + " -> " + newStr);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return diffs;
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String formatVal(String fieldName, Object val) {
        if (val == null) {
            return "空";
        }
        if ("status".equals(fieldName)) {
            if (val instanceof Integer) {
                return ((Integer) val == 0 || (Integer) val == 1) ? (((Integer) val == 1) ? "正常" : "禁用") : val.toString();
            }
            if ("ENABLE".equalsIgnoreCase(val.toString()) || "ENABLED".equalsIgnoreCase(val.toString())) {
                return "正常活跃";
            }
            if ("DISABLE".equalsIgnoreCase(val.toString()) || "DISABLED".equalsIgnoreCase(val.toString())) {
                return "账号冻结";
            }
        }
        if (val instanceof Boolean) {
            return (Boolean) val ? "是" : "否";
        }
        return val.toString();
    }
}
