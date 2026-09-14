package com.edumind.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.context.OperationLogContext;
import com.edumind.common.context.TenantContext;
import com.edumind.common.event.OperationLogEvent;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.common.utils.IpUtils;
import com.edumind.security.context.LoginUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 业务操作日志 AOP 切面（深度对标 E:\wu-admin LogAspect，升级为 Spring Event 异步事件驱动）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Before("@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, OperationLog controllerLog) {
        START_TIME.set(System.currentTimeMillis());
        OperationLogContext.clear();
    }

    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, OperationLog controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    @AfterThrowing(pointcut = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, OperationLog controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(JoinPoint joinPoint, OperationLog controllerLog, Exception e, Object jsonResult) {
        if (OperationLogContext.isSkipLog()) {
            return;
        }

        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null || tenantId <= 0) {
                tenantId = 1L;
            }

            String operUrl = "";
            String operIp = "127.0.0.1";
            String requestMethod = "POST";

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operUrl = request.getRequestURI();
                operIp = IpUtils.getClientIp(request);
                requestMethod = request.getMethod();
            }

            Long operUserId = LoginUserResolver.resolveUserId();
            String operName = resolveOperatorName(operUserId);

            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            String fullMethod = className + "." + methodName + "()";

            String customTitle = OperationLogContext.getTitle();
            String customAction = OperationLogContext.getAction();
            List<String> diffItems = OperationLogContext.getDiffItems();

            String finalTitle = resolveTitle(controllerLog, customTitle);
            Integer businessType = controllerLog.businessType().getValue();

            String operParam = null;
            if (controllerLog.isSaveRequestData()) {
                operParam = buildRequestParams(joinPoint, customAction, diffItems);
            }

            String jsonResultStr = null;
            if (controllerLog.isSaveResponseData() && jsonResult != null) {
                try {
                    String result = objectMapper.writeValueAsString(jsonResult);
                    jsonResultStr = truncate(result, 2000);
                } catch (Exception ignored) {
                }
            }

            int status = 0;
            String errorMsg = null;
            if (e != null) {
                status = 1;
                errorMsg = truncate(e.getMessage(), 2000);
            }

            Long startTime = START_TIME.get();
            long costTime = startTime != null ? (System.currentTimeMillis() - startTime) : 0L;

            OperationLogEvent.OperationLogPayload payload = OperationLogEvent.OperationLogPayload.builder()
                    .tenantId(tenantId)
                    .title(finalTitle)
                    .businessType(businessType)
                    .method(fullMethod)
                    .requestMethod(requestMethod)
                    .operUserId(operUserId)
                    .operName(operName)
                    .operUrl(operUrl)
                    .operIp(operIp)
                    .operParam(operParam)
                    .jsonResult(jsonResultStr)
                    .status(status)
                    .errorMsg(errorMsg)
                    .costTime(costTime)
                    .operTime(LocalDateTime.now())
                    .build();

            applicationEventPublisher.publishEvent(new OperationLogEvent(this, payload));
        } catch (Exception ex) {
            log.error("组装操作日志事件异常", ex);
        } finally {
            START_TIME.remove();
            OperationLogContext.clear();
        }
    }

    private String resolveTitle(OperationLog controllerLog, String customTitle) {
        if (StringUtils.hasText(customTitle)) {
            return customTitle;
        }
        if (StringUtils.hasText(controllerLog.title())) {
            if (StringUtils.hasText(controllerLog.module())) {
                return controllerLog.module() + " - " + controllerLog.title();
            }
            return controllerLog.title();
        }
        if (StringUtils.hasText(controllerLog.module())) {
            return controllerLog.module();
        }
        return "系统操作";
    }

    private String resolveOperatorName(Long operUserId) {
        LoginUser loginUser = UserContext.get();
        if (loginUser != null) {
            if (StringUtils.hasText(loginUser.getRealName())) {
                return loginUser.getRealName();
            }
            if (StringUtils.hasText(loginUser.getUsername())) {
                return loginUser.getUsername();
            }
        }
        if (StpUtil.isLogin()) {
            try {
                Object loginId = StpUtil.getLoginId();
                return loginId != null ? String.valueOf(loginId) : "系统用户";
            } catch (Exception ignored) {
            }
        }
        return operUserId != null ? ("用户#" + operUserId) : "匿名访问";
    }

    private String buildRequestParams(JoinPoint joinPoint, String customAction, List<String> diffItems) {
        try {
            Object[] args = joinPoint.getArgs();
            Map<String, Object> reqData = new LinkedHashMap<>();

            if (StringUtils.hasText(customAction)) {
                reqData.put("action", customAction);
            }
            if (diffItems != null && !diffItems.isEmpty()) {
                reqData.put("diffItems", diffItems);
            }

            List<Object> validArgs = new ArrayList<>();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg != null && !isFilterObject(arg)) {
                        validArgs.add(arg);
                    }
                }
            }

            if (!reqData.isEmpty()) {
                if (validArgs.size() == 1) {
                    reqData.put("params", validArgs.get(0));
                } else if (!validArgs.isEmpty()) {
                    reqData.put("params", validArgs);
                }
                String jsonStr = objectMapper.writeValueAsString(reqData);
                return truncate(maskSensitive(jsonStr), 2000);
            } else if (!validArgs.isEmpty()) {
                if (validArgs.size() == 1) {
                    return truncate(maskSensitive(objectMapper.writeValueAsString(validArgs.get(0))), 2000);
                } else {
                    return truncate(maskSensitive(objectMapper.writeValueAsString(validArgs)), 2000);
                }
            }
        } catch (Exception ex) {
            log.warn("序列化请求入参异常: {}", ex.getMessage());
        }
        return null;
    }

    private String maskSensitive(String json) {
        if (!StringUtils.hasText(json)) {
            return json;
        }
        return json.replaceAll("(\"(?i:(?:password|oldPassword|old_password|newPassword|new_password|confirmPassword|confirm_password|secretKey|secret_key|token|accessToken|access_token|secret|masterSecret|master_secret|apiKey|api_key|apiKeyCipher|api_key_cipher|cipher))\"\\s*:\\s*)\"[^\"]*\"", "$1\"******\"");
    }

    private boolean isFilterObject(Object obj) {
        if (obj instanceof MultipartFile || obj instanceof HttpServletRequest || obj instanceof HttpServletResponse) {
            return true;
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return MultipartFile.class.isAssignableFrom(clazz.getComponentType());
        }
        if (Collection.class.isAssignableFrom(clazz)) {
            for (Object item : (Collection<?>) obj) {
                if (item instanceof MultipartFile) {
                    return true;
                }
            }
        }
        if (Map.class.isAssignableFrom(clazz)) {
            for (Object value : ((Map<?, ?>) obj).values()) {
                if (value instanceof MultipartFile) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
