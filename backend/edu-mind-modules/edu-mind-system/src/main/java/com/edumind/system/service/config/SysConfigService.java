package com.edumind.system.service.config;

import com.edumind.infrastructure.mail.MailConfig;
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
}
