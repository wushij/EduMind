package com.edumind.system.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户展示对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO implements Serializable {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String status;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
    private String department;

    /** 契约别名 */
    public Long getUserId() {
        return id;
    }

    public String getNickname() {
        return realName;
    }
}
