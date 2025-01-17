package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.PhotoAlbumAdminDTO;
import com.boke.model.dto.PhotoAlbumDTO;
import com.boke.enums.FilePathEnum;
import com.boke.model.vo.ResultVO;
import com.boke.service.PhotoAlbumService;
import com.boke.strategy.context.UploadStrategyContext;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.PhotoAlbumVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name = "相册模块")
@RestController
public class PhotoAlbumController {

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @Autowired
    private PhotoAlbumService photoAlbumService;


    @OptLog(optType = UPLOAD)
    @Operation(summary =  "上传相册封面")
    @Parameter(name = "file", description = "相册封面", required = true)
    @PostMapping("/admin/photos/albums/upload")
    public ResultVO<String> savePhotoAlbumCover(MultipartFile file) {
        return ResultVO.ok(uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.PHOTO.getPath()));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @Operation(summary =  "保存或更新相册")
    @PostMapping("/admin/photos/albums")
    public ResultVO<?> saveOrUpdatePhotoAlbum(@Valid @RequestBody PhotoAlbumVO photoAlbumVO) {
        photoAlbumService.saveOrUpdatePhotoAlbum(photoAlbumVO);
        return ResultVO.ok();
    }

    @Operation(summary =  "查看后台相册列表")
    @GetMapping("/admin/photos/albums")
    public ResultVO<PageResultDTO<PhotoAlbumAdminDTO>> listPhotoAlbumBacks(ConditionVO conditionVO) {
        return ResultVO.ok(photoAlbumService.listPhotoAlbumsAdmin(conditionVO));
    }

    @Operation(summary =  "获取后台相册列表信息")
    @GetMapping("/admin/photos/albums/info")
    public ResultVO<List<PhotoAlbumDTO>> listPhotoAlbumBackInfos() {
        return ResultVO.ok(photoAlbumService.listPhotoAlbumInfosAdmin());
    }

    @Operation(summary =  "根据id获取后台相册信息")
    @Parameter(name = "albumId", description = "相册id", required = true, in = ParameterIn.PATH)
    @GetMapping("/admin/photos/albums/{albumId}/info")
    public ResultVO<PhotoAlbumAdminDTO> getPhotoAlbumBackById(@PathVariable("albumId") Integer albumId) {
        return ResultVO.ok(photoAlbumService.getPhotoAlbumByIdAdmin(albumId));
    }

    @OptLog(optType = DELETE)
    @Operation(summary =  "根据id删除相册")
    @Parameter(name = "albumId", description = "相册id", required = true, in = ParameterIn.PATH)
    @DeleteMapping("/admin/photos/albums/{albumId}")
    public ResultVO<?> deletePhotoAlbumById(@PathVariable("albumId") Integer albumId) {
        photoAlbumService.deletePhotoAlbumById(albumId);
        return ResultVO.ok();
    }

    @Operation(summary =  "获取相册列表")
    @GetMapping("/photos/albums")
    public ResultVO<List<PhotoAlbumDTO>> listPhotoAlbums() {
        return ResultVO.ok(photoAlbumService.listPhotoAlbums());
    }

}
