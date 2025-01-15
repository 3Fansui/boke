package com.boke.db.service;

import com.boke.model.dto.PageResultDTO;
import com.boke.model.dto.PhotoAdminDTO;
import com.boke.model.dto.PhotoDTO;
import com.boke.db.entity.Photo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.DeleteVO;
import com.boke.model.vo.PhotoInfoVO;
import com.boke.model.vo.PhotoVO;

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
