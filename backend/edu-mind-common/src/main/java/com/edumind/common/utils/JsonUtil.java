package com.edumind.common.utils;

import com.alibaba.fastjson2.JSON;

public class JsonUtil {
    public static String toJsonString(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }
}