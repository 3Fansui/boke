package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.AboutDTO;
import com.boke.model.dto.bokeAdminInfoDTO;
import com.boke.model.dto.bokeHomeInfoDTO;
import com.boke.model.dto.WebsiteConfigDTO;
import com.boke.enums.FilePathEnum;
import com.boke.model.vo.ResultVO;
import com.boke.db.service.bokeInfoService;
import com.boke.strategy.context.UploadStrategyContext;
import com.boke.model.vo.AboutVO;
import com.boke.model.vo.WebsiteConfigVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import static com.boke.constant.OptTypeConstant.UPDATE;
import static com.boke.constant.OptTypeConstant.UPLOAD;

@Api(tags = "aurora信息")
@RestController
public class bokeInfoController {

    @Autowired
    private bokeInfoService bokeInfoService;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @ApiOperation(value = "上报访客信息")
    @PostMapping("/report")
    public ResultVO<?> report() {
        bokeInfoService.report();
        return ResultVO.ok();
    }

    @ApiOperation(value = "获取系统信息")
    @GetMapping("/")
    public ResultVO<bokeHomeInfoDTO> getBlogHomeInfo() {
        return ResultVO.ok(bokeInfoService.getAuroraHomeInfo());
    }

    @ApiOperation(value = "获取系统后台信息")
    @GetMapping("/admin")
    public ResultVO<bokeAdminInfoDTO> getBlogBackInfo() {
        return ResultVO.ok(bokeInfoService.getAuroraAdminInfo());
    }

    @OptLog(optType = UPDATE)
    @ApiOperation(value = "更新网站配置")
    @PutMapping("/admin/website/config")
    public ResultVO<?> updateWebsiteConfig(@Valid @RequestBody WebsiteConfigVO websiteConfigVO) {
        bokeInfoService.updateWebsiteConfig(websiteConfigVO);
        return ResultVO.ok();
    }

    @ApiOperation(value = "获取网站配置")
    @GetMapping("/admin/website/config")
    public ResultVO<WebsiteConfigDTO> getWebsiteConfig() {
        return ResultVO.ok(bokeInfoService.getWebsiteConfig());
    }

    @ApiOperation(value = "查看关于我信息")
    @GetMapping("/about")
    public ResultVO<AboutDTO> getAbout() {
        return ResultVO.ok(bokeInfoService.getAbout());
    }

    @OptLog(optType = UPDATE)
    @ApiOperation(value = "修改关于我信息")
    @PutMapping("/admin/about")
    public ResultVO<?> updateAbout(@Valid @RequestBody AboutVO aboutVO) {
        bokeInfoService.updateAbout(aboutVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPLOAD)
    @ApiOperation(value = "上传博客配置图片")
    @ApiImplicitParam(name = "file", value = "图片", required = true, dataType = "MultipartFile")
    @PostMapping("/admin/config/images")
    public ResultVO<String> savePhotoAlbumCover(MultipartFile file) {
        return ResultVO.ok(uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.CONFIG.getPath()));
    }

}
