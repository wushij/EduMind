package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_preference")
public class UserPreferenceEntity {
    @TableId
    private Long userId;
    private String theme;
    private String language;
    private String defaultModel;
    private Integer enableRag;
    private Integer enableNotification;
    private String preferencesJson;
    private LocalDateTime updatedAt;
}
