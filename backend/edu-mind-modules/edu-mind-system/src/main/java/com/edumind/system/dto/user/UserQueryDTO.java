package com.edumind.system.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserQueryDTO implements Serializable {

    private String keyword;
    private String role;
    private String status;
    private Long page;
    private Long pageSize;
}
