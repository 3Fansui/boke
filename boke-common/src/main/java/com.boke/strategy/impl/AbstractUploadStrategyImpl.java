package com.boke.strategy.impl;

import com.boke.exception.BizException;
import com.boke.strategy.UploadStrategy;
import com.boke.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * 文件上传策略实现
 * 提供文件上传的通用逻辑，具体的上传实现由子类完成
 *
 * @author boke
 * @since 1.0
 */
@Service
public abstract class AbstractUploadStrategyImpl implements UploadStrategy {

    /**
     * 上传文件
     * 处理MultipartFile类型的文件上传，包含文件重复检查
     *
     * @param file 要上传的文件
     * @param path 文件存储路径
     * @return 文件访问URL
     * @throws BizException 当文件上传失败时抛出异常
     */
    @Override
    public String uploadFile(MultipartFile file, String path) {
        try {
            // 获取文件的MD5值作为文件名，防止重复
            String md5 = FileUtil.getMd5(file.getInputStream());
            // 获取文件扩展名
            String extName = FileUtil.getExtName(file.getOriginalFilename());
            // 组合最终的文件名
            String fileName = md5 + extName;

            // 检查文件是否已存在，不存在则上传
            if (!exists(path + fileName)) {
                upload(path, fileName, file.getInputStream());
            }

            // 返回文件的访问URL
            return getFileAccessUrl(path + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("文件上传失败");
        }
    }

    /**
     * 上传文件
     * 处理输入流方式的文件上传，直接上传不检查重复
     *
     * @param fileName    文件名
     * @param inputStream 文件输入流
     * @param path       文件存储路径
     * @return 文件访问URL
     * @throws BizException 当文件上传失败时抛出异常
     */
    @Override
    public String uploadFile(String fileName, InputStream inputStream, String path) {
        try {
            // 执行文件上传
            upload(path, fileName, inputStream);
            // 返回文件的访问URL
            return getFileAccessUrl(path + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("文件上传失败");
        }
    }

    /**
     * 判断文件是否存在
     * 由子类实现具体的存在性检查逻辑
     *
     * @param filePath 文件路径
     * @return true-存在，false-不存在
     */
    public abstract Boolean exists(String filePath);

    /**
     * 执行文件上传
     * 由子类实现具体的上传逻辑
     *
     * @param path        存储路径
     * @param fileName    文件名
     * @param inputStream 文件输入流
     * @throws IOException 当上传过程中发生IO异常时抛出
     */
    public abstract void upload(String path, String fileName, InputStream inputStream) throws IOException;

    /**
     * 获取文件访问URL
     * 由子类实现具体的URL获取逻辑
     *
     * @param filePath 文件路径
     * @return 可访问的文件URL
     */
    public abstract String getFileAccessUrl(String filePath);
}
