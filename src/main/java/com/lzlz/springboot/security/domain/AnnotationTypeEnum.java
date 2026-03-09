package com.lzlz.springboot.security.domain;

import lombok.Getter;

@Getter
public enum AnnotationTypeEnum {
    // 基本批注
    HIGHLIGHT("HIGHLIGHT", "高亮"),
    MARK("MARK", "标注"),
    NOTE("NOTE", "笔记"),
    // 扩展批注
    KEY_POINT("KEY_POINT", "重点批注"),
    DIFFICULTY("DIFFICULTY", "难点批注"),
    SUGGESTION("SUGGESTION", "建议类批注");

    private final String code;
    private final String desc;

    AnnotationTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AnnotationTypeEnum getByCode(String code) {
        for (AnnotationTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("无效的批注类型：" + code);
    }
}
