package com.edumind.system.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserUpdateDTO implements Serializable {

    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private String status;
    private List<Long> roleIds;
}
