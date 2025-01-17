package com.boke.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {

    // 收件人邮箱
    private String email;

    // 邮件主题
    private String subject;

    // 模板变量
    private Map<String, Object> commentMap;

    // 使用的模板名称 例如：comment.html
    private String template;

}
