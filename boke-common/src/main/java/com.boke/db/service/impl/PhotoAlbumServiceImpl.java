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

/**
 * 相册服务实现类
 * 处理相册的增删改查等相关业务逻辑
 *
 * @author boke
 * @since 1.0
 */
@Service
public class PhotoAlbumServiceImpl extends ServiceImpl<PhotoAlbumMapper, PhotoAlbum> implements PhotoAlbumService {

    /**
     * 相册数据访问层接口
     * 负责处理相册表(photo_album)的数据库操作
     */
    @Autowired
    private PhotoAlbumMapper photoAlbumMapper;

    /**
     * 照片数据访问层接口
     * 负责处理照片表(photo)的数据库操作
     */
    @Autowired
    private PhotoMapper photoMapper;

    /**
     * 保存或更新相册信息
     *
     * @param photoAlbumVO 相册信息VO对象
     * @throws BizException 当相册名称已存在时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdatePhotoAlbum(PhotoAlbumVO photoAlbumVO) {
        // 检查相册名是否已存在
        PhotoAlbum album = photoAlbumMapper.selectOne(new LambdaQueryWrapper<PhotoAlbum>()
                .select(PhotoAlbum::getId)
                .eq(PhotoAlbum::getAlbumName, photoAlbumVO.getAlbumName()));
        // 如果存在且不是当前编辑的相册，则抛出异常
        if (Objects.nonNull(album) && !album.getId().equals(photoAlbumVO.getId())) {
            throw new BizException("相册名已存在");
        }
        // 转换并保存相册信息
        PhotoAlbum photoAlbum = BeanCopyUtil.copyObject(photoAlbumVO, PhotoAlbum.class);
        this.saveOrUpdate(photoAlbum);
    }

    /**
     * 获取后台相册列表（分页）
     *
     * @param conditionVO 查询条件
     * @return 相册分页列表
     */
    @Override
    public PageResultDTO<PhotoAlbumAdminDTO> listPhotoAlbumsAdmin(ConditionVO conditionVO) {
        // 查询相册数量
        Integer count = photoAlbumMapper.selectCount(new LambdaQueryWrapper<PhotoAlbum>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        PhotoAlbum::getAlbumName, conditionVO.getKeywords())
                .eq(PhotoAlbum::getIsDelete, CommonConstant.FALSE));
        // 如果没有数据，返回空结果
        if (count == 0) {
            return new PageResultDTO<>();
        }
        // 查询相册列表
        List<PhotoAlbumAdminDTO> photoAlbumBacks = photoAlbumMapper.listPhotoAlbumsAdmin(
                PageUtil.getLimitCurrent(), PageUtil.getSize(), conditionVO);
        return new PageResultDTO<>(photoAlbumBacks, count);
    }

    /**
     * 获取后台相册列表信息
     *
     * @return 相册列表
     */
    @Override
    public List<PhotoAlbumDTO> listPhotoAlbumInfosAdmin() {
        // 查询未删除的相册列表
        List<PhotoAlbum> photoAlbums = photoAlbumMapper.selectList(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getIsDelete, CommonConstant.FALSE));
        return BeanCopyUtil.copyList(photoAlbums, PhotoAlbumDTO.class);
    }

    /**
     * 根据id获取后台相册信息
     *
     * @param albumId 相册id
     * @return 相册信息
     */
    @Override
    public PhotoAlbumAdminDTO getPhotoAlbumByIdAdmin(Integer albumId) {
        // 查询相册信息
        PhotoAlbum photoAlbum = photoAlbumMapper.selectById(albumId);
        // 查询相册下的照片数量
        Integer photoCount = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId)
                .eq(Photo::getIsDelete, CommonConstant.FALSE));
        // 转换为DTO并设置照片数量
        PhotoAlbumAdminDTO album = BeanCopyUtil.copyObject(photoAlbum, PhotoAlbumAdminDTO.class);
        album.setPhotoCount(photoCount);
        return album;
    }

    /**
     * 删除相册
     *
     * @param albumId 相册id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhotoAlbumById(Integer albumId) {
        // 查询相册下的照片数量
        Integer count = photoMapper.selectCount(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getAlbumId, albumId));
        if (count > 0) {
            // 如果相册下有照片，执行逻辑删除
            photoAlbumMapper.updateById(PhotoAlbum.builder()
                    .id(albumId)
                    .isDelete(CommonConstant.TRUE)
                    .build());
            // 同时删除相册下的所有照片
            photoMapper.update(new Photo(), new LambdaUpdateWrapper<Photo>()
                    .set(Photo::getIsDelete, CommonConstant.TRUE)
                    .eq(Photo::getAlbumId, albumId));
        } else {
            // 如果相册下没有照片，直接物理删除
            photoAlbumMapper.deleteById(albumId);
        }
    }

    /**
     * 获取前台相册列表
     *
     * @return 相册列表
     */
    @Override
    public List<PhotoAlbumDTO> listPhotoAlbums() {
        // 查询公开且未删除的相册列表，按ID降序排序
        List<PhotoAlbum> photoAlbumList = photoAlbumMapper.selectList(new LambdaQueryWrapper<PhotoAlbum>()
                .eq(PhotoAlbum::getStatus, PhotoAlbumStatusEnum.PUBLIC.getStatus())
                .eq(PhotoAlbum::getIsDelete, CommonConstant.FALSE)
                .orderByDesc(PhotoAlbum::getId));
        return BeanCopyUtil.copyList(photoAlbumList, PhotoAlbumDTO.class);
    }
}
