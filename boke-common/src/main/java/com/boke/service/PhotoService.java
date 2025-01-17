package com.boke.service;

import com.boke.model.dto.PageResultDTO;
import com.boke.model.dto.PhotoAdminDTO;
import com.boke.model.dto.PhotoAlbumAdminDTO;
import com.boke.model.dto.PhotoDTO;
import com.boke.entity.Photo;
import com.boke.model.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PhotoService extends IService<Photo> {

    PageResultDTO<PhotoAdminDTO> listPhotos(ConditionVO conditionVO);

    void updatePhoto(PhotoInfoVO photoInfoVO);

    void savePhotos(PhotoVO photoVO);

    void updatePhotosAlbum(PhotoVO photoVO);

    void updatePhotoDelete(DeleteVO deleteVO);

    void deletePhotos(List<Integer> photoIds);

    PhotoDTO listPhotosByAlbumId(Integer albumId);

}
