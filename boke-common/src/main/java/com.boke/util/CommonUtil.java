package com.boke.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用工具类
 * 提供常用的工具方法，包括：
 * 1. 邮箱格式验证
 * 2. 字符串处理
 * 3. 随机验证码生成
 * 4. 集合类型转换
 */
public class CommonUtil {

    /**
     * 验证邮箱格式是否正确
     * 使用正则表达式匹配邮箱格式
     *
     * 正则表达式说明：
     * ^        - 开始
     * \w+      - 一个或多个字母、数字、下划线
     * ((-\w+)|(\.\w+))*  - 可选的 -字母 或 .字母 组合
     * \@       - @符号
     * [A-Za-z0-9]+  - 一个或多个字母或数字
     * ((\\.|-)[A-Za-z0-9]+)*  - 可选的 .字母数字 或 -字母数字 组合
     * \.[A-Za-z0-9]+  - .后跟一个或多个字母或数字
     * $        - 结束
     *
     * @param username 要验证的邮箱地址
     * @return true-格式正确 false-格式错误
     */
    public static boolean checkEmail(String username) {
        String rule = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$";
        // 编译正则表达式
        Pattern p = Pattern.compile(rule);
        // 创建匹配器
        Matcher m = p.matcher(username);
        // 进行匹配并返回结果
        return m.matches();
    }

    /**
     * 获取字符串中括号内的内容
     * 例如："test(content)" 返回 "content"
     *
     * @param str 包含括号的字符串
     * @return 括号中的内容
     */
    public static String getBracketsContent(String str) {
        return str.substring(str.indexOf("(") + 1, str.indexOf(")"));
    }

    /**
     * 生成6位随机数字验证码
     * 使用Random类生成0-9之间的随机数
     *
     * @return 6位数字验证码
     */
    public static String getRandomCode() {
        StringBuilder str = new StringBuilder();
        Random random = new Random();
        // 生成6位随机数字
        for (int i = 0; i < 6; i++) {
            str.append(random.nextInt(10));
        }
        return str.toString();
    }

    /**
     * 将Object类型的List转换为指定类型的List
     * 用于处理泛型集合的类型转换
     *
     * @param obj 要转换的对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的List，如果转换失败返回空List
     */
    public static <T> List<T> castList(Object obj, Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return result;
    }

    /**
     * 将Object类型的Set转换为指定类型的Set
     * 用于处理泛型集合的类型转换
     *
     * @param obj 要转换的对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的Set，如果转换失败返回空Set
     */
    public static <T> Set<T> castSet(Object obj, Class<T> clazz) {
        Set<T> result = new HashSet<>();
        if (obj instanceof Set<?>) {
            for (Object o : (Set<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return result;
    }
}
