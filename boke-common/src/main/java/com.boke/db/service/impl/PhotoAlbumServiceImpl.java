package com.boke.db.service.impl;

import com.boke.constant.CommonConstant;
import com.boke.db.mapper.PhotoAlbumMapper;
import com.boke.db.mapper.PhotoMapper;
import com.boke.enums.PhotoAlbumStatusEnum;
import com.boke.model.dto.PhotoAlbumAdminDTO;
import com.boke.model.dto.PhotoAlbumDTO;
import com.boke.db.entity.Photo;
import com.boke.db.entity.PhotoAlbum;
import com.boke.exception.BizException;
import com.boke.db.service.PhotoAlbumService;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.PhotoAlbumVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.boke.constant.CommonConstant.FALSE;
import static com.boke.constant.CommonConstant.TRUE;
import static com.boke.enums.PhotoAlbumStatusEnum.PUBLIC;

@Service
public class PhotoAlbumServiceImpl extends ServiceImpl<PhotoAlbumMapper, PhotoAlbum> implements PhotoAlbumService {

    @Autowired
    private PhotoAlbumMapper photoAlbumMapper;

    @Autowired
    private PhotoMapper photoMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdatePhotoAlbum(PhotoAlbumVO photoAlbumVO) {
        PhotoAlbum album = photoAlbumMapper.selectOne(new LambdaQueryWrapper<PhotoAlbum>()
                .select(PhotoAlbum::getId)
                .eq(PhotoAlbum::getAlbumName, photoAlbumVO.getAlbumName()));
        if (Objects.nonNull(album) && !album.getId().equals(photoAlbumVO.getId())) {
            throw new BizException("相册名已存在");
        }
        PhotoAlbum photoAlbum = BeanCopyUtil.copyObject(photoAlbumVO, PhotoAlbum.class);
        this.saveOrUpdate(photoAlbum);
    }

    @Override
    public PageResultDTO<PhotoAlbumAdminDTO> listPhotoAlbumsAdmin(ConditionVO conditionVO) {
        Integer count = photoAlbumMapper.selectCount(new LambdaQueryWrapper<PhotoAlbum>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), PhotoAlbum::getAlbumName, conditionVO.getKeywords())
                .eq(PhotoAlbum::getIsDelete, CommonConstant.FALSE));
        if (count == 0) {
            return new PageResultDTO<>();
        }
        List<PhotoAlbumAdminDTO> photoAlbumBacks = photoAlbumMapper.listPhotoAlbumsAdmin(PageUtil.getLimitCurrent(), PageUtil.getSize(), conditionVO);
        return new PageResultDTO<>(photoAlbumBacks, count);
    }

    @Override
    public List<PhotoAlbumDTO> listPhotoAlbumInfosAdmin() {
        List<PhotoAlbum> photoAlbums = photoAlbumMapper.selectList(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getIsDelete, CommonConstant.FALSE));
        return BeanCopyUtil.copyList(photoAlbums, PhotoAlbumDTO.class);
    }

    @Override
    public PhotoAlbumAdminDTO getPhotoAlbumByIdAdmin(Integer albumId) {
        PhotoAlbum photoAlbum = photoAlbumMapper.selectById(albumId);
        Integer photoCount = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId)
                .eq(Photo::getIsDelete, CommonConstant.FALSE));
        PhotoAlbumAdminDTO album = BeanCopyUtil.copyObject(photoAlbum, PhotoAlbumAdminDTO.class);
        album.setPhotoCount(photoCount);
        return album;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhotoAlbumById(Integer albumId) {
        Integer count = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId));
        if (count > 0) {
            photoAlbumMapper.updateById(PhotoAlbum.builder()
                    .id(albumId)
                    .isDelete(CommonConstant.TRUE)
                    .build());
            photoMapper.update(new Photo(), new LambdaUpdateWrapper<Photo>()
                    .set(Photo::getIsDelete, CommonConstant.TRUE)
                    .eq(Photo::getAlbumId, albumId));
        } else {
            photoAlbumMapper.deleteById(albumId);
        }
    }

    @Override
    public List<PhotoAlbumDTO> listPhotoAlbums() {
        List<PhotoAlbum> photoAlbumList = photoAlbumMapper.selectList(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getStatus, PhotoAlbumStatusEnum.PUBLIC.getStatus())
                .eq(PhotoAlbum::getIsDelete, CommonConstant.FALSE)
                .orderByDesc(PhotoAlbum::getId));
        return BeanCopyUtil.copyList(photoAlbumList, PhotoAlbumDTO.class);
    }

}
