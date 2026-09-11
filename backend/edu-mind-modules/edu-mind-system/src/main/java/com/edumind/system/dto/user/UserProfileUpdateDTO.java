package com.edumind.system.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileUpdateDTO implements Serializable {

    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
}
