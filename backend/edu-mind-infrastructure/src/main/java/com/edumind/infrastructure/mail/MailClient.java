package com.edumind.infrastructure.mail;

import com.edumind.common.exception.BusinessException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 动态 SMTP 邮件客户端：负责与 SMTP 服务器握手通信，组装双通道防进垃圾箱邮件报文并发送
 */
@Slf4j
@Component
public class MailClient {

    /**
     * 发送验证码邮件（异步执行，避免阻塞前端请求，未配置发信服务时记录日志告警）
     */
    public void sendVerificationCodeAsync(MailConfig config, String toEmail, String scene, String code, int expireMin) {
        if (config == null || Boolean.FALSE.equals(config.getEnabled()) || config.getUsername() == null || config.getUsername().isBlank()) {
            log.warn("未启用或未配置 SMTP 发件服务，跳过验证码邮件投递 [toEmail={}, scene={}]", toEmail, scene);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                sendMimeMail(config, toEmail, scene, code, expireMin);
                log.info("邮箱安全验证码邮件投递成功 [toEmail={}, scene={}]", toEmail, scene);
            } catch (Exception ex) {
                log.error("发送邮箱验证码邮件失败 [toEmail={}, scene={}], 原因: {}", toEmail, scene, ex.getMessage(), ex);
            }
        });
    }

    /**
     * 发送测试邮件（同步执行，抛出明确异常以便管理端连通性排查）
     */
    public void testSend(MailConfig config, String toEmail) {
        if (config == null || config.getUsername() == null || config.getUsername().isBlank()) {
            throw new BusinessException("请先配置发信邮箱账号与授权码");
        }
        try {
            sendMimeMail(config, toEmail, "test", "952701", 10);
            log.info("SMTP 连通性测试邮件投递成功 [toEmail={}]", toEmail);
        } catch (Exception ex) {
            log.error("SMTP 连通性测试失败 [toEmail={}]: {}", toEmail, ex.getMessage(), ex);
            throw new BusinessException("SMTP 发送失败: " + ex.getMessage());
        }
    }

    /**
     * 组装标准 MIME 双通道报文并执行投递
     */
    private void sendMimeMail(MailConfig config, String toEmail, String scene, String code, int expireMin) throws Exception {
        JavaMailSenderImpl mailSender = createSender(config);
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        String platformName = config.getFromName() != null && !config.getFromName().isBlank()
                ? config.getFromName()
                : "智教云 · EduMind";

        MailTemplateBuilder.SceneContent content = MailTemplateBuilder.sceneContentFor(scene, platformName);
        String plainText = MailTemplateBuilder.buildPlain(platformName, content.sceneTitle(), content.actionText(), code, expireMin);
        String htmlText = MailTemplateBuilder.buildHtml(platformName, content.sceneTitle(), content.actionText(), code, expireMin);

        // MimeMessageHelper(mimeMessage, true, "UTF-8") 设为 multipart 模式
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

        // 发信人（带昵称 MIME Q-Encoding 编码）
        String fromPersonal = MimeUtility.encodeText(platformName, StandardCharsets.UTF_8.name(), "B");
        helper.setFrom(new InternetAddress(config.getUsername(), fromPersonal, StandardCharsets.UTF_8.name()));
        helper.setTo(toEmail);
        helper.setSubject(content.subject());

        // 核心防进垃圾箱 (Anti-Spam) 标头注入
        // 1. RFC 3834: 标识为自动化事务邮件，避免被各大邮箱当成营销推广/订阅邮件，并阻止假期自动回复
        mimeMessage.setHeader("Auto-Submitted", "auto-generated");
        // 2. 发件合法指纹
        mimeMessage.setHeader("X-Mailer", "EduMind-Secure-Mailer/2.0");
        // 3. 规范 Message-ID，避免缺失 Message-ID 触发的反垃圾评分扣减
        String messageId = String.format("<%d-%s@edumind.cn>", System.currentTimeMillis(), UUID.randomUUID().toString());
        mimeMessage.setHeader("Message-ID", messageId);
        // 4. 标准时间戳
        SimpleDateFormat rfc1123Format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        mimeMessage.setHeader("Date", rfc1123Format.format(new Date()));

        // 5. 双通道格式注入：setText(plainText, htmlText) 自动产生 multipart/alternative
        helper.setText(plainText, htmlText);

        // 投递
        mailSender.send(mimeMessage);
    }

    /**
     * 根据配置动态构建 JavaMailSender
     */
    private JavaMailSenderImpl createSender(MailConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort() != null ? config.getPort() : 465);
        sender.setUsername(config.getUsername());
        sender.setPassword(config.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        boolean isSsl = Boolean.TRUE.equals(config.getUseSsl()) || (config.getPort() != null && config.getPort() == 465);
        if (isSsl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.socketFactory.port", String.valueOf(config.getPort() != null ? config.getPort() : 465));
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        // 超时防挂起保护 (10秒)
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return sender;
    }
}
