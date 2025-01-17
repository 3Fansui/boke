package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.*;
import com.boke.enums.FilePathEnum;
import com.boke.service.ArticleService;
import com.boke.strategy.context.ArticleImportStrategyContext;
import com.boke.strategy.context.UploadStrategyContext;
import com.boke.model.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name = "文章模块")
@RestController
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @Autowired
    private ArticleImportStrategyContext articleImportStrategyContext;

    @Operation(summary = "获取置顶和推荐文章")
    @GetMapping("/articles/topAndFeatured")
    public ResultVO<TopAndFeaturedArticlesDTO> listTopAndFeaturedArticles() {
        return ResultVO.ok(articleService.listTopAndFeaturedArticles());
    }

    @Operation(summary = "获取所有文章")
    @GetMapping("/articles/all")
    public ResultVO<PageResultDTO<ArticleCardDTO>> listArticles() {
        return ResultVO.ok(articleService.listArticles());
    }

    @Operation(summary = "根据分类id获取文章")
    @GetMapping("/articles/categoryId")
    public ResultVO<PageResultDTO<ArticleCardDTO>> getArticlesByCategoryId(@RequestParam Integer categoryId) {
        return ResultVO.ok(articleService.listArticlesByCategoryId(categoryId));
    }

    @Operation(summary = "根据id获取文章")
    @GetMapping("/articles/{articleId}")
    public ResultVO<ArticleDTO> getArticleById(@PathVariable("articleId") Integer articleId) {
        return ResultVO.ok(articleService.getArticleById(articleId));
    }

    @Operation(summary = "校验文章访问密码")
    @PostMapping("/articles/access")
    public ResultVO<?> accessArticle(@Valid @RequestBody ArticlePasswordVO articlePasswordVO) {
        articleService.accessArticle(articlePasswordVO);
        return ResultVO.ok();
    }

    @Operation(summary = "根据标签id获取文章")
    @GetMapping("/articles/tagId")
    public ResultVO<PageResultDTO<ArticleCardDTO>> listArticlesByTagId(@RequestParam Integer tagId) {
        return ResultVO.ok(articleService.listArticlesByTagId(tagId));
    }

    @Operation(summary = "获取所有文章归档")
    @GetMapping("/archives/all")
    public ResultVO<PageResultDTO<ArchiveDTO>> listArchives() {
        return ResultVO.ok(articleService.listArchives());
    }

    @Operation(summary = "获取后台文章")
    @GetMapping("/admin/articles")
    public ResultVO<PageResultDTO<ArticleAdminDTO>> listArticlesAdmin(ConditionVO conditionVO) {
        return ResultVO.ok(articleService.listArticlesAdmin(conditionVO));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @Operation(summary = "保存和修改文章")
    @PostMapping("/admin/articles")
    public ResultVO<?> saveOrUpdateArticle(@Valid @RequestBody ArticleVO articleVO) {
        articleService.saveOrUpdateArticle(articleVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "修改文章是否置顶和推荐")
    @PutMapping("/admin/articles/topAndFeatured")
    public ResultVO<?> updateArticleTopAndFeatured(@Valid @RequestBody ArticleTopFeaturedVO articleTopFeaturedVO) {
        articleService.updateArticleTopAndFeatured(articleTopFeaturedVO);
        return ResultVO.ok();
    }

    @Operation(summary = "删除或者恢复文章")
    @PutMapping("/admin/articles")
    public ResultVO<?> updateArticleDelete(@Valid @RequestBody DeleteVO deleteVO) {
        articleService.updateArticleDelete(deleteVO);
        return ResultVO.ok();
    }

    @OptLog(optType = DELETE)
    @Operation(summary = "物理删除文章")
    @DeleteMapping("/admin/articles/delete")
    public ResultVO<?> deleteArticles(@RequestBody List<Integer> articleIds) {
        articleService.deleteArticles(articleIds);
        return ResultVO.ok();
    }

    @OptLog(optType = UPLOAD)
    @Operation(summary = "上传文章图片")

    @Parameter(name = "file", description = "文章图片", required = true, in = ParameterIn.PATH)
    @PostMapping("/admin/articles/images")
    public ResultVO<String> saveArticleImages(MultipartFile file) {
        return ResultVO.ok(uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.ARTICLE.getPath()));
    }

    @Operation(summary = "根据id查看后台文章")

    @Parameters(value = {
            @Parameter(name = "articleId", description = "文章id", required = true, in = ParameterIn.PATH),
    })
    @GetMapping("/admin/articles/{articleId}")
    public ResultVO<ArticleAdminViewDTO> getArticleBackById(@PathVariable("articleId") Integer articleId) {
        return ResultVO.ok(articleService.getArticleByIdAdmin(articleId));
    }

    @OptLog(optType = UPLOAD)
    @Operation(summary = "导入文章")
    @PostMapping("/admin/articles/import")
    public ResultVO<?> importArticles(MultipartFile file, @RequestParam(required = false) String type) {
        articleImportStrategyContext.importArticles(file, type);
        return ResultVO.ok();
    }

    @OptLog(optType = EXPORT)
    @Operation(summary = "导出文章")
    @Parameter(name = "articleIdList", description = "文章id", required = true, in = ParameterIn.PATH)
    @PostMapping("/admin/articles/export")
    public ResultVO<List<String>> exportArticles(@RequestBody List<Integer> articleIds) {
        return ResultVO.ok(articleService.exportArticles(articleIds));
    }

    @Operation(summary = "搜索文章")
    @GetMapping("/articles/search")
    public ResultVO<List<ArticleSearchDTO>> listArticlesBySearch(ConditionVO condition) {
        return ResultVO.ok(articleService.listArticlesBySearch(condition));
    }

}
