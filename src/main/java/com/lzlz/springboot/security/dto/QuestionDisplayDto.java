package com.lzlz.springboot.security.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionDisplayDto {
    private Long id;            // 题目ID
    private Integer type;       // 题目类型 (1:单选, 2:多选, 3:判断, 4:简答)
    private String content;     // 题干内容 (例如: "Java中int占用几个字节?")
    private String options;     // 选项 (如果是选择题，存JSON或特殊分隔字符串，如 "A.1;B.2;C.4")
    private Integer score;      // 这道题在作业中的分值
    private Integer sortOrder;  // 题目排序
}