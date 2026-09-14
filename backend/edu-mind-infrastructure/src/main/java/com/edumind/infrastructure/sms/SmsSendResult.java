package com.edumind.infrastructure.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSendResult {

    private boolean success;
    private String provider;
    private String message;
    private String bizId;

    public static SmsSendResult ok(String provider, String message, String bizId) {
        return SmsSendResult.builder().success(true).provider(provider).message(message).bizId(bizId).build();
    }

    public static SmsSendResult fail(String provider, String message) {
        return SmsSendResult.builder().success(false).provider(provider).message(message).build();
    }
}
