package com.boke.msg.util;


import com.boke.model.dto.EmailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 邮件发送服务组件
 * 使用 Spring Mail 和 Thymeleaf 模板引擎发送 HTML 格式邮件
 */
@Component
public class EmailUtil {

    @Value("${spring.mail.username}")
    private String email;

    /**
     * Spring Boot 邮件发送工具
     * 用于创建和发送邮件
     */
    @Autowired
    private JavaMailSender javaMailSender;


    /**
     * Thymeleaf 模板引擎
     * 用于处理 HTML 邮件模板
     */
    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 发送 HTML 格式的邮件
     *
     * 该方法实现以下功能：
     * 1. 创建邮件消息对象
     * 2. 设置邮件模板变量
     * 3. 处理 HTML 模板
     * 4. 配置邮件发送信息
     * 5. 发送邮件
     *
     * @param emailDTO 邮件信息数据传输对象，包含：
     *                - email: 收件人邮箱
     *                - subject: 邮件主题
     *                - template: 模板名称
     *                - commentMap: 模板变量映射
     */
    public void sendHtmlMail(EmailDTO emailDTO) {
        try {
            // 创建 MIME 邮件对象
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            // 创建 MIME 邮件帮助器
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

            // 创建 Thymeleaf 上下文对象
            //Thymeleaf 是一个用于处理和生成 HTML、XML、JavaScript、CSS
            // 以及纯文本的 Java 模板引擎。它特别适合于在 Spring 框架中使用，但也可以作为独立的模板引擎使用。
            Context context = new Context();
            // 设置模板变量，用于替换模板中的占位符
            context.setVariables(emailDTO.getCommentMap());

            // 处理邮件模板，生成 HTML 内容
            String process = templateEngine.process(emailDTO.getTemplate(), context);

            // 设置邮件基本信息
            mimeMessageHelper.setFrom(email);           // 设置发件人
            mimeMessageHelper.setTo(emailDTO.getEmail()); // 设置收件人
            mimeMessageHelper.setSubject(emailDTO.getSubject()); // 设置邮件主题
            mimeMessageHelper.setText(process, true);   // 设置邮件内容，true 表示支持 HTML 格式

            // 发送邮件
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            // 邮件发送异常处理
            e.printStackTrace();
        }
    }

}
