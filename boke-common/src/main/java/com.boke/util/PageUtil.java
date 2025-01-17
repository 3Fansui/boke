package com.boke.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Objects;

/**
 * PageUtil 工具类用于管理分页信息。
 * 使用 ThreadLocal 存储和获取分页参数，确保线程安全。
 */
public class PageUtil {

    // 使用 ThreadLocal 存储分页信息
    private static final ThreadLocal<Page<?>> PAGE_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前分页信息。
     *
     * @param page 包含分页参数的 Page 对象
     */
    public static void setCurrentPage(Page<?> page) {
        PAGE_HOLDER.set(page);
    }

    /**
     * 获取当前分页信息。
     * 如果当前线程没有分页信息，则创建一个新的 Page 对象并设置到 ThreadLocal 中。
     *
     * @return 当前线程的 Page 对象
     */
    public static Page<?> getPage() {
        Page<?> page = PAGE_HOLDER.get();
        if (Objects.isNull(page)) {
            setCurrentPage(new Page<>());
        }
        return PAGE_HOLDER.get();
    }

    /**
     * 获取当前页码。
     *
     * @return 当前页码
     */
    public static Long getCurrent() {
        return getPage().getCurrent();
    }

    /**
     * 获取每页的记录数。
     *
     * @return 每页的记录数
     */
    public static Long getSize() {
        return getPage().getSize();
    }

    /**
     * 计算当前分页的起始位置。
     * 计算公式为 (当前页码 - 1) * 每页记录数。
     *
     * @return 当前分页的起始位置
     */
    public static Long getLimitCurrent() {
        return (getCurrent() - 1) * getSize();
    }

    /**
     * 移除当前线程的分页信息，防止内存泄漏。
     */
    public static void remove() {
        PAGE_HOLDER.remove();
    }
}
