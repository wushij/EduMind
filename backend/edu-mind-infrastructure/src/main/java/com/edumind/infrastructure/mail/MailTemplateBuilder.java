package com.edumind.infrastructure.mail;

import java.time.Year;

/**
 * 邮件内容生成器：负责业务场景文案映射、纯文本备选流与全客户端自适应高保真卡片模板构建
 * 核心特性：
 * 1. 纯文本 + HTML 双通道（规避纯 HTML 触发的反垃圾邮件扣分）
 * 2. 540px 全客户端安全表格布局（100% 兼容 Outlook, Foxmail, Gmail, QQ 邮箱, 163 网易等）
 * 3. 官方安全验证防伪提示与敏感凭据保护警示
 */
public final class MailTemplateBuilder {

    private MailTemplateBuilder() {
    }

    /**
     * 场景文案承载对象
     */
    public record SceneContent(String subject, String sceneTitle, String actionText) {
    }

    /**
     * 按业务场景获取主题与描述文案
     */
    public static SceneContent sceneContentFor(String scene, String platformName) {
        String name = (platformName != null && !platformName.isBlank()) ? platformName : "智教云 · EduMind";
        if (scene == null) {
            scene = "bind";
        }
        return switch (scene.toLowerCase().trim()) {
            case "login" -> new SceneContent(
                    String.format("【%s】快捷登录安全验证码", name),
                    "快捷登录",
                    String.format("您正在登录【%s】AI 智能教学赋能平台", name)
            );
            case "bind" -> new SceneContent(
                    String.format("【%s】邮箱绑定安全验证码", name),
                    "邮箱绑定",
                    "您正在进行账户邮箱绑定与安全认证操作"
            );
            case "resetpwd", "modifypwd" -> new SceneContent(
                    String.format("【%s】密码重置安全验证码", name),
                    "重置密码",
                    "您正在申请重置账户登录密码"
            );
            case "test" -> new SceneContent(
                    String.format("【%s】SMTP 邮件服务连通性测试", name),
                    "服务连通性测试",
                    "您正在后台控制台发起 SMTP 发信通道连通性验证"
            );
            default -> new SceneContent(
                    String.format("【%s】安全验证码", name),
                    "安全验证",
                    "您正在进行身份安全核验操作"
            );
        };
    }

    /**
     * 构建纯文本兜底（Plain Text Alternative Part）
     */
    public static String buildPlain(String platformName, String sceneTitle, String actionText, String code, int expireMin) {
        String name = (platformName != null && !platformName.isBlank()) ? platformName : "智教云 · EduMind";
        return String.format("""
                【%s】%s安全验证码
                
                您好！%s，本次操作的专属验证码为：
                👉 %s 👈
                
                * 验证码有效期为 %d 分钟，请尽快完成核验。
                * 安全防范提示：验证码为敏感安全凭据，官方团队绝不会主动向您索取，请勿向他人泄露。
                * 如非本人操作，请忽略此邮件，您的账号依然安全。
                --------------------------------------------------
                %s · 探索智能教育 · 赋能教学创新
                本邮件由 EduMind 安全中枢系统自动投递，请勿直接回复。
                """, name, sceneTitle, actionText, code, expireMin, name);
    }

    /**
     * 构建高保真富文本 HTML（Rich HTML Part）
     */
    public static String buildHtml(String platformName, String sceneTitle, String actionText, String code, int expireMin) {
        String name = (platformName != null && !platformName.isBlank()) ? platformName : "智教云 · EduMind";
        int currentYear = Year.now().getValue();

        return String.format("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <meta http-equiv="X-UA-Compatible" content="IE=edge">
                  <title>%s 安全验证码</title>
                </head>
                <body style="margin:0;padding:28px 0;background-color:#f4f6f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;-webkit-font-smoothing:antialiased;-webkit-text-size-adjust:100%%;-ms-text-size-adjust:100%%;">
                
                  <!-- 居中外层容器：自适应手机端与桌面端，无多余黑边 -->
                  <div style="max-width:540px;margin:0 auto;padding:0 14px;">
                    
                    <!-- 核心卡片容器：全客户端统一圆角、纯白底色与精细边框 -->
                    <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width:540px;background-color:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 4px 20px rgba(0,0,0,0.06);border-collapse:separate;">
                      
                      <!-- 头部深空星夜 Banner -->
                      <tr>
                        <td style="padding:22px 26px 18px;background-color:#0b0f19;background-image:linear-gradient(135deg, #0b0f19 0%%, #162447 50%%, #0f172a 100%%);border-bottom:2px solid #1677FF;">
                          <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                            <tr>
                              <td align="left" style="vertical-align:middle;">
                                <table border="0" cellpadding="0" cellspacing="0">
                                  <tr>
                                    <td style="font-size:24px;line-height:1;padding-right:10px;vertical-align:middle;">🎓</td>
                                    <td style="vertical-align:middle;">
                                      <span style="font-family:'Cabinet Grotesk','Outfit',Cinzel,-apple-system,sans-serif;font-size:20px;font-weight:800;color:#ffffff;letter-spacing:1px;display:inline-block;line-height:1.2;">EduMind</span>
                                    </td>
                                  </tr>
                                </table>
                                <div style="font-size:11.5px;color:#94a3b8;margin-top:5px;letter-spacing:0.5px;line-height:1.3;padding-left:34px;">智教云 · AI 智能教学赋能平台</div>
                              </td>
                              <td align="right" style="vertical-align:middle;">
                                <span style="display:inline-block;padding:4px 12px;background-color:rgba(22,119,255,0.18);border:1px solid rgba(22,119,255,0.45);border-radius:20px;color:#60a5fa;font-size:11px;font-weight:700;letter-spacing:0.5px;white-space:nowrap;">
                                  官方安全认证
                                </span>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                
                      <!-- 正文内容区域 -->
                      <tr>
                        <td style="padding:28px 26px 24px;background-color:#ffffff;">
                          <h2 style="margin:0 0 12px;color:#0f172a;font-size:16px;font-weight:700;">尊敬的平台用户，您好：</h2>
                          <p style="margin:0 0 20px;color:#475569;font-size:14px;line-height:1.7;">
                            %s，本次操作的专属身份验证码如下：
                          </p>
                
                          <!-- 验证码高光卡片 -->
                          <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color:#eff6ff;background-image:linear-gradient(135deg, #eff6ff 0%%, #f0fdf4 45%%, #fefce8 100%%);border:2px dashed #3b82f6;border-radius:12px;margin-bottom:20px;">
                            <tr>
                              <td align="center" style="padding:22px 14px;">
                                <div style="font-size:12px;font-weight:700;color:#1d4ed8;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:6px;">【%s】专属安全验证码</div>
                                <div style="font-size:36px;font-weight:900;letter-spacing:10px;color:#1d4ed8;font-family:Consolas,Monaco,'Lucida Console',monospace;line-height:1.2;margin:6px 0;padding-left:10px;">%s</div>
                                <div style="margin-top:8px;font-size:13px;color:#1e3a8a;font-weight:600;">
                                  ⏱ 验证码有效期为 <strong style="color:#2563eb;">%d 分钟</strong>，请尽快完成验证
                                </div>
                              </td>
                            </tr>
                          </table>
                
                          <!-- 安全提示卡片 -->
                          <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color:#fffbeb;border:1px solid #fef3c7;border-left:4px solid #f59e0b;border-radius:8px;">
                            <tr>
                              <td style="padding:12px 14px;color:#92400e;font-size:12.5px;line-height:1.6;">
                                🛡️ <strong>安全防范提示：</strong>验证码为敏感安全凭据，官方团队绝不会主动向您索取，请勿泄露给任何人。如非本人操作，请忽略此邮件，您的账号依然安全。
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                
                      <!-- 页脚 -->
                      <tr>
                        <td style="padding:20px 24px;background-color:#0f172a;text-align:center;border-top:1px solid #1e293b;">
                          <div style="color:#cbd5e1;font-size:12px;font-weight:700;">
                            %s
                          </div>
                          <div style="color:#94a3b8;font-size:11px;margin-top:4px;">
                            探索智能教育 · 赋能教学创新 · 系统安全中枢自动投递，请勿直接回复
                          </div>
                          <div style="color:#475569;font-size:11px;margin-top:4px;">
                            © %d EduMind. All rights reserved.
                          </div>
                        </td>
                      </tr>
                    </table>
                    <!-- 核心卡片结束 -->
                
                  </div>
                </body>
                </html>
                """,
                escapeHtml(name),
                escapeHtml(actionText),
                escapeHtml(sceneTitle),
                escapeHtml(code),
                expireMin,
                escapeHtml(name),
                currentYear
        );
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
