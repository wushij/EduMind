package com.edumind.statistics.vo.learning;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WrongBookListVO {
    private Long total;
    private List<WrongBookItemVO> list = new ArrayList<>();
}
