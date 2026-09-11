package com.edumind.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String email;
    private List<String> roles;
    private List<String> permissions;
}