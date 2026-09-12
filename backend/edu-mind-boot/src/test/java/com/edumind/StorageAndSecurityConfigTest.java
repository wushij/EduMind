package com.edumind;

import com.edumind.infrastructure.oss.StorageConfig;
import com.edumind.infrastructure.oss.impl.RoutingFileStorageService;
import com.edumind.security.config.DynamicSecurityConfigService;
import com.edumind.security.config.SecurityProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StorageAndSecurityConfigTest {

    @Test
    @DisplayName("验证本地存储与路由门面工作与自测连通性")
    void testLocalStorageAndRoutingFacade() throws Exception {
        RoutingFileStorageService routingFacade = new RoutingFileStorageService();
        StorageConfig config = new StorageConfig();
        config.setType("local");
        config.setLocalPath("data/test_edumind");
        routingFacade.reload(config);

        Assertions.assertEquals("local", routingFacade.getStorageType());
        Assertions.assertTrue(routingFacade.testConnection());

        // 验证分类路径上传与下载：如 avatars/admin/test.txt
        String content = "Hello EduMind Avatar Class Separation";
        String objectName = "avatars/admin/test.txt";
        String accessUrl = routingFacade.uploadFile("edumind", objectName,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "text/plain");

        Assertions.assertNotNull(accessUrl);
        Assertions.assertTrue(accessUrl.contains("avatars/admin/test.txt"));

        try (InputStream is = routingFacade.getFile("edumind", objectName)) {
            Assertions.assertNotNull(is);
            String readBack = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Assertions.assertEquals(content, readBack);
        }

        // 清理测试文件
        routingFacade.deleteFile("edumind", objectName);
    }

    @Test
    @DisplayName("验证动态安全配置内存默认值与热更新")
    void testDynamicSecurityConfig() {
        SecurityProperties props = new SecurityProperties();
        props.setSmEnabled(false);
        props.setTimestampSkewMs(300000L);

        DynamicSecurityConfigService securityService = new DynamicSecurityConfigService(props, null);

        // 默认配置断言
        Assertions.assertFalse(securityService.isTimestampEnabled());
        Assertions.assertEquals(300000L, securityService.getTimestampWindowMs());
        Assertions.assertFalse(securityService.isNonceEnabled());
        Assertions.assertFalse(securityService.isSm3SignEnabled());
        Assertions.assertFalse(securityService.isSm4EncryptEnabled());
        Assertions.assertFalse(securityService.isDisableDevtool());

        // 模拟热更新
        Map<String, Object> req = new HashMap<>();
        req.put("timestampEnabled", true);
        req.put("timestampWindowMs", 120000L);
        req.put("nonceEnabled", true);
        req.put("sm3SignEnabled", true);
        req.put("sm4EncryptEnabled", true);
        req.put("disableDevtool", true);
        req.put("captchaAfterFailures", 5);

        securityService.updateConfig(req);

        // 检验更新后即时生效
        Assertions.assertTrue(securityService.isTimestampEnabled());
        Assertions.assertEquals(120000L, securityService.getTimestampWindowMs());
        Assertions.assertTrue(securityService.isNonceEnabled());
        Assertions.assertTrue(securityService.isSm3SignEnabled());
        Assertions.assertTrue(securityService.isSm4EncryptEnabled());
        Assertions.assertTrue(securityService.isDisableDevtool());
        Assertions.assertEquals(5, securityService.getCaptchaAfterFailures());
    }
}
