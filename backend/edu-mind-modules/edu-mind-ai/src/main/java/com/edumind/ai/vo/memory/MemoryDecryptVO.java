package com.edumind.ai.vo.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryDecryptVO {
    private Long id;
    private String decryptedContent;
    private Integer keyVersion;
    private String algorithm;
}
