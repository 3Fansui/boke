package com.boke.db.service.impl;


import com.boke.constant.CommonConstant;
import com.boke.db.mapper.PhotoMapper;
import com.boke.db.service.PhotoAlbumService;
import com.boke.enums.PhotoAlbumStatusEnum;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.dto.PhotoAdminDTO;
import com.boke.model.dto.PhotoDTO;
import com.boke.db.entity.Photo;
import com.boke.db.entity.PhotoAlbum;
import com.boke.exception.BizException;
import com.boke.db.service.PhotoService;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.DeleteVO;
import com.boke.model.vo.PhotoInfoVO;
import com.boke.model.vo.PhotoVO;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 照片服务实现类
 * 处理照片的增删改查等相关业务逻辑
 *
 * @author boke
 * @since 1.0
 */
@Service
public class PhotoServiceImpl extends ServiceImpl<PhotoMapper, Photo> implements PhotoService {

    /**
     * 照片数据访问层接口
     * 负责照片表的基础CRUD操作
     */
    @Autowired
    private PhotoMapper photoMapper;

    /**
     * 相册服务接口
     * 用于处理照片与相册的关联操作
     */
    @Autowired
    private PhotoAlbumService photoAlbumService;


    /**
     * 分页查询照片列表
     * 支持按相册ID筛选和软删除状态筛选
     *
     * @param conditionVO 查询条件，包含相册ID和删除状态
     * @return 分页结果，包含照片信息列表
     */
    @Override
    public PageResultDTO<PhotoAdminDTO> listPhotos(ConditionVO conditionVO) {
        // 创建分页对象
        Page<Photo> page = new Page<>(PageUtil.getCurrent(), PageUtil.getSize());
        // 执行分页查询
        Page<Photo> photoPage = photoMapper.selectPage(page, new LambdaQueryWrapper<Photo>()
                // 如果指定了相册ID，则按相册ID筛选
                .eq(Objects.nonNull(conditionVO.getAlbumId()),
                        Photo::getAlbumId, conditionVO.getAlbumId())
                // 按删除状态筛选
                .eq(Photo::getIsDelete, conditionVO.getIsDelete())
                // 按ID降序排序
                .orderByDesc(Photo::getId)
                // 按更新时间降序排序
                .orderByDesc(Photo::getUpdateTime));
        // 将实体转换为DTO
        List<PhotoAdminDTO> photos = BeanCopyUtil.copyList(photoPage.getRecords(), PhotoAdminDTO.class);
        // 返回分页结果
        return new PageResultDTO<>(photos, (int) photoPage.getTotal());
    }

    /**
     * 更新照片信息
     *
     * @param photoInfoVO 照片信息VO对象，包含需要更新的照片信息
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhoto(PhotoInfoVO photoInfoVO) {
        // 将VO对象转换为实体对象并更新
        Photo photo = BeanCopyUtil.copyObject(photoInfoVO, Photo.class);
        photoMapper.updateById(photo);
    }

    /**
     * 批量保存照片
     * 为每个照片URL创建一个新的照片记录
     *
     * @param photoVO 照片信息VO对象，包含相册ID和照片URL列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void savePhotos(PhotoVO photoVO) {
        // 将照片URL列表转换为照片实体列表
        List<Photo> photoList = photoVO.getPhotoUrls().stream().map(item -> Photo.builder()
                        .albumId(photoVO.getAlbumId())    // 设置相册ID
                        .photoName(IdWorker.getIdStr())   // 生成唯一的照片名称
                        .photoSrc(item)                   // 设置照片URL
                        .build())
                .collect(Collectors.toList());
        // 批量保存照片
        this.saveBatch(photoList);
    }

    /**
     * 更新照片所属相册
     * 批量修改照片的相册ID
     *
     * @param photoVO 照片信息VO对象，包含照片ID列表和目标相册ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhotosAlbum(PhotoVO photoVO) {
        // 将照片ID列表转换为照片实体列表，更新相册ID
        List<Photo> photoList = photoVO.getPhotoIds().stream().map(item -> Photo.builder()
                        .id(item)                        // 设置照片ID
                        .albumId(photoVO.getAlbumId())   // 设置新的相册ID
                        .build())
                .collect(Collectors.toList());
        // 批量更新照片
        this.updateBatchById(photoList);
    }

    /**
     * 更新照片的删除状态
     * 支持批量操作，同时处理相关相册的删除状态
     *
     * @param deleteVO 删除信息VO对象，包含照片ID列表和删除状态
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updatePhotoDelete(DeleteVO deleteVO) {
        // 批量更新照片的删除状态
        List<Photo> photoList = deleteVO.getIds().stream().map(item -> Photo.builder()
                        .id(item)
                        .isDelete(deleteVO.getIsDelete())
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(photoList);

        // 如果是恢复照片（设置为未删除状态）
        if (deleteVO.getIsDelete().equals(CommonConstant.FALSE)) {
            // 查询这些照片所属的相册，并将相册也设置为未删除状态
            List<PhotoAlbum> photoAlbumList = photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                            .select(Photo::getAlbumId)
                            .in(Photo::getId, deleteVO.getIds())
                            .groupBy(Photo::getAlbumId))
                    .stream()
                    .map(item -> PhotoAlbum.builder()
                            .id(item.getAlbumId())
                            .isDelete(CommonConstant.FALSE)
                            .build())
                    .collect(Collectors.toList());
            photoAlbumService.updateBatchById(photoAlbumList);
        }
    }

    /**
     * 物理删除照片
     * 从数据库中永久删除指定的照片记录
     *
     * @param photoIds 需要删除的照片ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePhotos(List<Integer> photoIds) {
        photoMapper.deleteBatchIds(photoIds);
    }

    /**
     * 获取相册中的照片列表
     * 查询指定相册的照片，同时返回相册信息
     *
     * @param albumId 相册ID
     * @return 包含相册信息和照片列表的DTO对象
     * @throws BizException 当相册不存在或不可访问时抛出异常
     */
    @Override
    public PhotoDTO listPhotosByAlbumId(Integer albumId) {
        // 查询相册信息，确保相册存在且是公开的
        PhotoAlbum photoAlbum = photoAlbumService.getOne(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getId, albumId)
                .eq(PhotoAlbum::getIsDelete, CommonConstant.FALSE)
                .eq(PhotoAlbum::getStatus, PhotoAlbumStatusEnum.PUBLIC.getStatus()));

        // 如果相册不存在或不可访问，抛出异常
        if (Objects.isNull(photoAlbum)) {
            throw new BizException("相册不存在");
        }

        // 分页查询相册中的照片
        Page<Photo> page = new Page<>(PageUtil.getCurrent(), PageUtil.getSize());
        List<String> photos = photoMapper.selectPage(page, new LambdaQueryWrapper<Photo>()
                        .select(Photo::getPhotoSrc)
                        .eq(Photo::getAlbumId, albumId)
                        .eq(Photo::getIsDelete, CommonConstant.FALSE)
                        .orderByDesc(Photo::getId))
                .getRecords()
                .stream()
                .map(Photo::getPhotoSrc)
                .collect(Collectors.toList());

        // 构建并返回结果DTO
        return PhotoDTO.builder()
                .photoAlbumCover(photoAlbum.getAlbumCover())
                .photoAlbumName(photoAlbum.getAlbumName())
                .photos(photos)
                .build();
    }

}
