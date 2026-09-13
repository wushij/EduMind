package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberVO implements Serializable {
    private Long id;
    private Long userId;
    private String studentNo;
    private String name;
    private String role;
    private String avatar;
    private Integer masteryRate;
    private String lastActive;
}
