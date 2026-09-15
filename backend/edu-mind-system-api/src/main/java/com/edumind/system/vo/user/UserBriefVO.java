package com.edumind.system.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户跨模块只读简要信息（API 契约层）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBriefVO implements Serializable {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
}
