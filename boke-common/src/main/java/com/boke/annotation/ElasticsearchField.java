package com.boke.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 文档字段注解
 * 对象的id字段不能加此注解
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ElasticsearchField {
    String fieldName();
}