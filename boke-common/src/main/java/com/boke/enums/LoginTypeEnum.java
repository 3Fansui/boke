package com.boke.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    EMAIL(1, "邮箱登录", ""),

    QQ(2, "QQ登录", "qqLoginStrategyImpl"),

    Gitee(3, "Gitee登录", "");

    private final Integer type;

    private final String desc;

    private final String strategy;

}
