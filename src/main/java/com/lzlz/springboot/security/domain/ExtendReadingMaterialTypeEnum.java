package com.lzlz.springboot.security.domain;

import lombok.Getter;

@Getter
public enum ExtendReadingMaterialTypeEnum {
    URL("URL", "网页链接"),
    TEXT("TEXT", "文本内容"),
    FILE("FILE", "文件");

    private final String code;
    private final String desc;

    ExtendReadingMaterialTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ExtendReadingMaterialTypeEnum getByCode(String code) {
        for (ExtendReadingMaterialTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("无效的拓展阅读材料类型：" + code);
    }
}
