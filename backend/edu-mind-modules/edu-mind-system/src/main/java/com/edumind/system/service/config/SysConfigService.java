package com.edumind.system.service.config;

import com.edumind.infrastructure.mail.MailConfig;
import com.edumind.infrastructure.sms.SmsConfig;
import com.edumind.system.dto.config.MailConfigDTO;
import com.edumind.system.dto.config.MailTestDTO;
import com.edumind.system.vo.config.MailConfigVO;

/**
 * 系统全局参数配置业务接口
 */
public interface SysConfigService {

    /**
     * 获取邮件服务原始配置（系统内部调用，包含真实密码）
     */
    MailConfig getMailConfig();

    /**
     * 获取短信服务原始配置（系统内部调用，包含真实密钥）
     */
    SmsConfig getSmsConfig();

    /**
     * 获取邮件服务展示配置（管理端调用，密码脱敏）
     */
    MailConfigVO getMailConfigVO();

    /**
     * 更新邮件服务系统配置
     */
    void updateMailConfig(MailConfigDTO dto);

    /**
     * 测试邮件服务连通性
     */
    void testMail(MailTestDTO dto);

    /**
     * 根据键获取配置值
     */
    String getConfigValue(String key);

    /**
     * 获取存储引擎展示配置（密钥脱敏）
     */
    com.edumind.system.vo.config.StorageConfigVO getStorageConfigVO();

    /**
     * 更新存储引擎系统配置并热重载驱动
     */
    com.edumind.system.vo.config.StorageConfigVO updateStorageConfig(com.edumind.system.dto.config.StorageConfigDTO dto);

    /**
     * 测试存储引擎连通性
     */
    boolean testStorageConfig(com.edumind.system.dto.config.StorageConfigDTO dto);

    /**
     * 获取全链路安全防护配置
     */
    com.edumind.system.vo.config.SecurityConfigVO getSecurityConfigVO();

    /**
     * 更新全链路安全防护配置并热生效
     */
    com.edumind.system.vo.config.SecurityConfigVO updateSecurityConfig(com.edumind.system.dto.config.SecurityConfigDTO dto);

    /**
     * 获取全部系统配置分组列表
     */
    java.util.List<com.edumind.system.vo.config.SysConfigGroupVO> listAllGroups();

    /**
     * 根据分组编码获取配置
     */
    com.edumind.system.vo.config.SysConfigGroupVO getByGroupCode(String groupCode);

    /**
     * 更新指定分组配置
     */
    void updateConfigGroup(String groupCode, String configValueJson);

    /**
     * 测试发送短信
     */
    boolean testSms(com.edumind.system.dto.config.TestSmsDTO dto);

    /**
     * 获取最近短信发送日志
     */
    java.util.List<com.edumind.system.vo.config.SmsLogVO> getRecentSmsLogs(int limit);

    /**
     * 分页查询短信发送日志
     */
    com.edumind.common.api.PageResult<com.edumind.system.vo.config.SmsLogVO> pageSmsLogs(int page, int size, String phone, Integer status);

    /**
     * 创建测试支付订单
     */
    java.util.Map<String, String> testPayment(com.edumind.system.dto.config.TestPaymentDTO dto);

    /**
     * 获取最近邮件发送日志
     */
    java.util.List<com.edumind.system.vo.config.EmailLogVO> getRecentEmailLogs(int limit);

    /**
     * 分页查询邮件发送日志
     */
    com.edumind.common.api.PageResult<com.edumind.system.vo.config.EmailLogVO> pageEmailLogs(int page, int size, String email, Integer status);

    /**
     * 获取系统可用角色选项（供注册默认角色、AI配额配置）
     */
    java.util.List<com.edumind.system.vo.config.RoleOptionVO> getRoleOptions();

    /**
     * 获取系统可用用户选项（供注册审核人配置）
     */
    java.util.List<com.edumind.system.vo.config.UserOptionVO> getUserOptions();
}
