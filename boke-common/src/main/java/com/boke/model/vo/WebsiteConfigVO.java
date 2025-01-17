package com.boke.model.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "网站配置")
public class WebsiteConfigVO {

    @Schema(name = "name", description = "网站名称", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String name;

    @Schema(name = "nickName", description = "网站作者昵称", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String englishName;

    @Schema(name = "author", description = "网站作者", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String author;

    @Schema(name = "avatar", description = "网站头像", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String authorAvatar;

    @Schema(name = "description", description = "网站作者介绍", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String authorIntro;

    @Schema(name = "logo", description = "网站logo", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String logo;

    @Schema(name = "multiLanguage", description = "多语言", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer multiLanguage;

    @Schema(name = "notice", description = "网站公告", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String notice;

    @Schema(name = "websiteCreateTime", description = "网站创建时间", requiredMode = Schema.RequiredMode.REQUIRED, type = "LocalDateTime")
    private String websiteCreateTime;

    @Schema(name = "beianNumber", description = "网站备案号", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String beianNumber;

    @Schema(name = "qqLogin", description = "QQ登录", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer qqLogin;

    @Schema(name = "github", description = "github", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String github;

    @Schema(name = "gitee", description = "gitee", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String gitee;

    @Schema(name = "qq", description = "qq", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String qq;

    @Schema(name = "weChat", description = "微信", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String weChat;

    @Schema(name = "weibo", description = "微博", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String weibo;

    @Schema(name = "csdn", description = "csdn", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String csdn;

    @Schema(name = "zhihu", description = "zhihu", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String zhihu;

    @Schema(name = "juejin", description = "juejin", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String juejin;

    @Schema(name = "twitter", description = "twitter", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String twitter;

    @Schema(name = "stackoverflow", description = "stackoverflow", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String stackoverflow;

    @Schema(name = "touristAvatar", description = "游客头像", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String touristAvatar;

    @Schema(name = "userAvatar", description = "用户头像", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String userAvatar;

    @Schema(name = "isCommentReview", description = "是否评论审核", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer isCommentReview;

    @Schema(name = "isEmailNotice", description = "是否邮箱通知", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer isEmailNotice;

    @Schema(name = "isReward", description = "是否打赏", requiredMode = Schema.RequiredMode.REQUIRED, type = "Integer")
    private Integer isReward;

    @Schema(name = "weiXinQRCode", description = "微信二维码", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String weiXinQRCode;

    @Schema(name = "alipayQRCode", description = "支付宝二维码", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String alipayQRCode;

    @Schema(name = "favicon", description = "favicon", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String favicon;

    @Schema(name = "websiteTitle", description = "网页标题", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String websiteTitle;

    @Schema(name = "gonganBeianNumber", description = "公安部备案编号", requiredMode = Schema.RequiredMode.REQUIRED, type = "String")
    private String gonganBeianNumber;

}
