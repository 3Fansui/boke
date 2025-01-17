package com.boke.strategy.context;

import com.boke.strategy.UploadStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

import static com.boke.enums.UploadModeEnum.getStrategy;

/**
 * 文件上传策略上下文
 * 根据配置的上传模式选择不同的上传策略
 *
 * @author boke
 * @since 1.0
 */
@Service
public class UploadStrategyContext {

    /**
     * 上传模式
     * 通过配置文件注入，可选值如：oss、cos、local等
     */
    @Value("${upload.mode}")
    private String uploadMode;

    /**
     * 上传策略Map
     * key: 策略名称
     * value: 对应的上传策略实现类
     * 通过Spring自动注入所有实现了UploadStrategy接口的Bean
     */
    @Autowired
    private Map<String, UploadStrategy> uploadStrategyMap;

    /**
     * 执行文件上传策略
     * 用于处理MultipartFile类型的文件上传
     *
     * @param file 要上传的文件
     * @param path 文件存储路径
     * @return 文件访问URL
     */
    public String executeUploadStrategy(MultipartFile file, String path) {
        // 根据配置的上传模式获取对应的策略实现并执行上传
        return uploadStrategyMap.get(getStrategy(uploadMode))
                .uploadFile(file, path);
    }

    /**
     * 执行文件上传策略
     * 用于处理输入流方式的文件上传
     *
     * @param fileName    文件名
     * @param inputStream 文件输入流
     * @param path       文件存储路径
     * @return 文件访问URL
     */
    public String executeUploadStrategy(String fileName, InputStream inputStream, String path) {
        // 根据配置的上传模式获取对应的策略实现并执行上传
        return uploadStrategyMap.get(getStrategy(uploadMode))
                .uploadFile(fileName, inputStream, path);
    }

}
