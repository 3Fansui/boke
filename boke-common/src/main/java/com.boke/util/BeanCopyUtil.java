package com.boke.util;


import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean拷贝工具类
 * 用于实现对象之间属性的复制
 *
 * @author your_name
 * @since your_version
 */
public class BeanCopyUtil {

    /**
     * 复制对象
     *
     * @param source 源对象
     * @param target 目标对象的类
     * @param <T>    目标对象类型
     * @return 目标对象实例
     */
    public static <T> T copyObject(Object source, Class<T> target) {
        T temp = null;
        try {
            // 创建目标对象实例
            // 推荐方法：使用 target.getDeclaredConstructor().newInstance()
            // 这种方法可以调用类的无参或有参构造函数，并且需要捕获和处理可能的异常。
            temp = target.newInstance();
            // 如果源对象不为空，进行属性复制
            if (null != source) {
                BeanUtils.copyProperties(source, temp);
            }
        } catch (Exception e) {
            // 打印异常堆栈信息
            e.printStackTrace();
        }
        return temp;
    }

    /**
     * 复制列表
     *
     * @param source 源对象列表
     * @param target 目标对象的类
     * @param <T>    目标对象类型
     * @param <S>    源对象类型
     * @return 目标对象列表
     */
    public static <T, S> List<T> copyList(List<S> source, Class<T> target) {
        // 创建目标对象列表
        List<T> list = new ArrayList<>();
        // 如果源列表不为空且长度大于0，进行复制
        if (null != source && source.size() > 0) {
            // 遍历源列表，复制每个对象
            for (Object obj : source) {
                list.add(BeanCopyUtil.copyObject(obj, target));
            }
        }
        return list;
    }

}