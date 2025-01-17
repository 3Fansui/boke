package com.boke.service;

import com.boke.model.dto.PhotoAlbumAdminDTO;
import com.boke.model.dto.PhotoAlbumDTO;
import com.boke.entity.PhotoAlbum;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.PhotoAlbumVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PhotoAlbumService extends IService<PhotoAlbum> {

    void saveOrUpdatePhotoAlbum(PhotoAlbumVO photoAlbumVO);

    PageResultDTO<PhotoAlbumAdminDTO> listPhotoAlbumsAdmin(ConditionVO condition);

    List<PhotoAlbumDTO> listPhotoAlbumInfosAdmin();

    PhotoAlbumAdminDTO getPhotoAlbumByIdAdmin(Integer albumId);

    void deletePhotoAlbumById(Integer albumId);

    List<PhotoAlbumDTO> listPhotoAlbums();

}
