package com.xiaoliao.api.m2.enums;

import lombok.Getter;

/**
 * 认知域枚举
 */
@Getter
public enum CognitiveDomainEnum {

    MEMORY("memory", "记忆", "今天练了记忆力"),
    ATTENTION("attention", "注意力", "今天练了注意力和反应速度"),
    REASON("reason", "推理", "今天练了推理和归类能力"),
    LANGUAGE("language", "语言", "今天练了语言表达能力");

    private final String code;
    private final String label;
    private final String tip;

    CognitiveDomainEnum(String code, String label, String tip) {
        this.code = code;
        this.label = label;
        this.tip = tip;
    }

    public static CognitiveDomainEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CognitiveDomainEnum e : values()) {
            if (e.code.equalsIgnoreCase(code) || e.label.equals(code)) {
                return e;
            }
        }
        return null;
    }

    /** 通俗提示文案，用于局后反馈 */
    public static String tipOf(String cognitiveDomain) {
        CognitiveDomainEnum e = fromCode(cognitiveDomain);
        return e != null ? e.getTip() : "今天大脑又动起来了";
    }
}
