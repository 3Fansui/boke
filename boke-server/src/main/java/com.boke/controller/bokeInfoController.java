package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.AboutDTO;
import com.boke.model.dto.bokeAdminInfoDTO;
import com.boke.model.dto.bokeHomeInfoDTO;
import com.boke.model.dto.WebsiteConfigDTO;
import com.boke.enums.FilePathEnum;
import com.boke.model.vo.ResultVO;
import com.boke.service.bokeInfoService;
import com.boke.strategy.context.UploadStrategyContext;
import com.boke.model.vo.AboutVO;
import com.boke.model.vo.WebsiteConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



import static com.boke.constant.OptTypeConstant.UPDATE;
import static com.boke.constant.OptTypeConstant.UPLOAD;

@Tag(name =  "aurora信息")
@RestController
public class bokeInfoController {

    @Autowired
    private bokeInfoService bokeInfoService;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @Operation(summary = "上报访客信息")
    @PostMapping("/report")
    public ResultVO<?> report() {
        bokeInfoService.report();
        return ResultVO.ok();
    }

    @Operation(summary = "获取系统信息")
    @GetMapping("/")
    public ResultVO<bokeHomeInfoDTO> getBlogHomeInfo() {
        return ResultVO.ok(bokeInfoService.getAuroraHomeInfo());
    }

    @Operation(summary = "获取系统后台信息")
    @GetMapping("/admin")
    public ResultVO<bokeAdminInfoDTO> getBlogBackInfo() {
        return ResultVO.ok(bokeInfoService.getAuroraAdminInfo());
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "更新网站配置")
    @PutMapping("/admin/website/config")
    public ResultVO<?> updateWebsiteConfig(@Valid @RequestBody WebsiteConfigVO websiteConfigVO) {
        bokeInfoService.updateWebsiteConfig(websiteConfigVO);
        return ResultVO.ok();
    }

    @Operation(summary = "获取网站配置")
    @GetMapping("/admin/website/config")
    public ResultVO<WebsiteConfigDTO> getWebsiteConfig() {
        return ResultVO.ok(bokeInfoService.getWebsiteConfig());
    }

    @Operation(summary = "查看关于我信息")
    @GetMapping("/about")
    public ResultVO<AboutDTO> getAbout() {
        return ResultVO.ok(bokeInfoService.getAbout());
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "修改关于我信息")
    @PutMapping("/admin/about")
    public ResultVO<?> updateAbout(@Valid @RequestBody AboutVO aboutVO) {
        bokeInfoService.updateAbout(aboutVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPLOAD)
    @Operation(summary = "上传博客配置图片")
    @Parameter(name = "file", description = "图片", required = true)
    @PostMapping("/admin/config/images")
    public ResultVO<String> savePhotoAlbumCover(MultipartFile file) {
        return ResultVO.ok(uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.CONFIG.getPath()));
    }

}
