package com.lzlz.springboot.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student_paper_details") // 对应数据库表名
public class StudentPaperDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("record_id")
    private Long recordId;      // 关联的主记录ID

    @TableField("question_id")
    private String questionId;  // 题目ID

    @TableField("student_answer")
    private String studentAnswer; // 学生填写的答案

    @TableField("is_correct")
    private Boolean isCorrect;    // 是否正确

    @TableField("score_gained")
    private Integer scoreGained;  // 该题实际得分
}