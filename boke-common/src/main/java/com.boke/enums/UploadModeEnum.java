package com.boke.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件上传模式枚举
 * 定义系统支持的文件上传方式及其对应的策略实现类
 *
 * @author boke
 * @since 1.0
 */
@Getter                    // Lombok注解，自动生成getter方法
@AllArgsConstructor        // Lombok注解，自动生成全参构造函数
public enum UploadModeEnum {

    /**
     * 阿里云OSS对象存储
     * mode: 配置文件中的模式标识
     * strategy: 对应的策略实现类Bean名称
     */
    OSS("oss", "ossUploadStrategyImpl"),

    /**
     * MinIO对象存储
     * mode: 配置文件中的模式标识
     * strategy: 对应的策略实现类Bean名称
     */
    MINIO("minio", "minioUploadStrategyImpl");

    /**
     * 上传模式
     * 与配置文件中的upload.mode值对应
     */
    private final String mode;

    /**
     * 策略类名称
     * 对应具体的上传策略实现类的Bean名称
     */
    private final String strategy;

    /**
     * 根据上传模式获取对应的策略类名称
     *
     * @param mode 上传模式
     * @return 策略类名称，如果没有找到对应的模式则返回null
     */
    public static String getStrategy(String mode) {
        // 遍历所有枚举值
        for (UploadModeEnum value : UploadModeEnum.values()) {
            // 比较模式是否匹配
            if (value.getMode().equals(mode)) {
                // 返回对应的策略类名称
                return value.getStrategy();
            }
        }
        // 如果没有找到匹配的模式，返回null
        return null;
    }
}
