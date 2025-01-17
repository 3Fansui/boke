package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.enums.FilePathEnum;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.dto.PhotoAdminDTO;
import com.boke.model.dto.PhotoDTO;
import com.boke.service.PhotoService;
import com.boke.model.vo.*;
import com.boke.strategy.context.UploadStrategyContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name =  "照片模块")
@RestController
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @OptLog(optType = UPLOAD)
    @Operation(summary =  "上传照片")
    @Parameter(name = "file", description = "照片", required = true)
    @PostMapping("/admin/photos/upload")
    public ResultVO<String> savePhotoAlbumCover(MultipartFile file) {
        return ResultVO.ok(uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.PHOTO.getPath()));
    }

    @Operation(summary =  "根据相册id获取照片列表")
    @GetMapping("/admin/photos")
    public ResultVO<PageResultDTO<PhotoAdminDTO>> listPhotos(ConditionVO conditionVO) {
        return ResultVO.ok(photoService.listPhotos(conditionVO));
    }

    @OptLog(optType = UPDATE)
    @Operation(summary =  "更新照片信息")
    @PutMapping("/admin/photos")
    public ResultVO<?> updatePhoto(@Valid @RequestBody PhotoInfoVO photoInfoVO) {
        photoService.updatePhoto(photoInfoVO);
        return ResultVO.ok();
    }

    @OptLog(optType = SAVE)
    @Operation(summary =  "保存照片")
    @PostMapping("/admin/photos")
    public ResultVO<?> savePhotos(@Valid @RequestBody PhotoVO photoVO) {
        photoService.savePhotos(photoVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary =  "移动照片相册")
    @PutMapping("/admin/photos/album")
    public ResultVO<?> updatePhotosAlbum(@Valid @RequestBody PhotoVO photoVO) {
        photoService.updatePhotosAlbum(photoVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary =  "更新照片删除状态")
    @PutMapping("/admin/photos/delete")
    public ResultVO<?> updatePhotoDelete(@Valid @RequestBody DeleteVO deleteVO) {
        photoService.updatePhotoDelete(deleteVO);
        return ResultVO.ok();
    }

    @OptLog(optType = DELETE)
    @Operation(summary =  "删除照片")
    @DeleteMapping("/admin/photos")
    public ResultVO<?> deletePhotos(@RequestBody List<Integer> photoIds) {
        photoService.deletePhotos(photoIds);
        return ResultVO.ok();
    }

    @Operation(summary =  "根据相册id查看照片列表")
    @GetMapping("/albums/{albumId}/photos")
    public ResultVO<PhotoDTO> listPhotosByAlbumId(@PathVariable("albumId") Integer albumId) {
        return ResultVO.ok(photoService.listPhotosByAlbumId(albumId));
    }

}
