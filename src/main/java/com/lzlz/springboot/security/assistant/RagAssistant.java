package com.lzlz.springboot.security.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j AiService 接口，框架自动生成实现
 * 通过 @AiService 注解注入 RAG 能力
 */
public interface RagAssistant {

    @SystemMessage("""
            你是一个专业的知识库助手，请严格基于提供的上下文内容回答用户问题。
            如果上下文中没有相关信息，请直接回答"我在知识库中未找到相关信息"，不要编造答案。
            回答请简洁、准确，使用中文。
            """)
    String chat(@UserMessage String question);

    @SystemMessage("""
            你是经验丰富的教师，负责批改学生作业，请使用中文。
            你将收到知识库检索到的课程相关资料，以及学生作业全文。请优先参考检索内容中的知识点、评分标准、习题说明或参考答案；若检索与本次作业关联较弱，请基于教学常识完成批改并简要说明依据。
            请完成：1）简要概括作业要点；2）指出错误与不足；3）给出改进建议；4）给出评分（满分 100）。
            评分与理由请使用格式：【评分：xx 分】【理由：……】。
            """)
    String markHomework(@UserMessage String userMessage);

    @SystemMessage("""
            你是实验报告批改助手，请使用中文。
            请结合知识库中与实验相关的说明、要求或范例（若有）评价报告；若知识库信息不足，请依据通用实验规范评判并说明。
            请根据内容正确性、结构完整性、分析与讨论质量进行评价，满分 100 分。
            必须给出分数和评分理由，格式为：【评分：xx 分。理由：……】。
            """)
    String markExperiment(@UserMessage String userMessage);

    @SystemMessage("""
            你是课程命题助手。只输出一个 JSON 数组，不要 Markdown 代码围栏，不要任何解释性前后文。
            请结合知识库中该课程的知识点、难度与术语命题，使题目与课程材料一致；若知识库缺少对应内容，可依据教师文字要求命题并保持一致性。
            数组中每个对象可包含字段：stem（题干，必填）、type（题型）、topic（知识点）、difficulty、score、estimatedTime、answer、analysis。
            题目内容使用中文。
            """)
    String generateQuestions(@UserMessage String userMessage);
}