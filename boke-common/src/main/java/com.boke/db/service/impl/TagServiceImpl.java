package com.boke.db.service.impl;


import com.boke.db.mapper.ArticleTagMapper;
import com.boke.db.mapper.TagMapper;
import com.boke.model.dto.TagAdminDTO;
import com.boke.model.dto.TagDTO;
import com.boke.db.entity.ArticleTag;
import com.boke.db.entity.Tag;
import com.boke.exception.BizException;
import com.boke.db.service.TagService;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.TagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 标签服务实现类
 * 继承ServiceImpl实现基础的CRUD操作
 * 实现TagService接口，提供标签相关的业务逻辑处理
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    /**
     * 标签数据访问接口
     */
    @Autowired
    private TagMapper tagMapper;

    /**
     * 文章标签关联数据访问接口
     */
    @Autowired
    private ArticleTagMapper articleTagMapper;

    /**
     * 获取所有标签列表
     *
     * @return 返回标签DTO列表
     */
    @Override
    public List<TagDTO> listTags() {
        return tagMapper.listTags();
    }

    /**
     * 获取排名前十的热门标签
     *
     * @return 返回前十标签DTO列表
     */
    @Override
    public List<TagDTO> listTopTenTags() {
        return tagMapper.listTopTenTags();
    }

    /**
     * 分页查询后台标签列表
     *
     * @param conditionVO 查询条件，包含关键词等参数
     * @return 分页结果，包含标签管理DTO列表和总数
     */
    @SneakyThrows
    @Override
    public PageResultDTO<TagAdminDTO> listTagsAdmin(ConditionVO conditionVO) {
        // 查询标签数量
        Integer count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        Tag::getTagName,
                        conditionVO.getKeywords()));
        // 如果没有数据，返回空结果
        if (count == 0) {
            return new PageResultDTO<>();
        }
        // 分页查询标签列表
        List<TagAdminDTO> tags = tagMapper.listTagsAdmin(
                PageUtil.getLimitCurrent(),
                PageUtil.getSize(),
                conditionVO);
        return new PageResultDTO<>(tags, count);
    }

    /**
     * 搜索后台标签列表
     * 根据关键词模糊查询标签
     *
     * @param conditionVO 查询条件，包含关键词
     * @return 标签管理DTO列表
     */
    @SneakyThrows
    @Override
    public List<TagAdminDTO> listTagsAdminBySearch(ConditionVO conditionVO) {
        // 根据关键词查询标签列表
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        Tag::getTagName,
                        conditionVO.getKeywords())
                .orderByDesc(Tag::getId));
        // 将实体转换为DTO
        return BeanCopyUtil.copyList(tags, TagAdminDTO.class);
    }

    /**
     * 保存或更新标签
     * 保存前会检查标签名是否已存在
     *
     * @param tagVO 标签视图对象，包含标签信息
     * @throws BizException 当标签名已存在时抛出异常
     */
    @Override
    public void saveOrUpdateTag(TagVO tagVO) {
        // 检查标签名是否已存在
        Tag existTag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .select(Tag::getId)
                .eq(Tag::getTagName, tagVO.getTagName()));
        // 如果标签名存在且不是当前编辑的标签，则抛出异常
        if (Objects.nonNull(existTag) && !existTag.getId().equals(tagVO.getId())) {
            throw new BizException("标签名已存在");
        }
        // 转换并保存标签
        Tag tag = BeanCopyUtil.copyObject(tagVO, Tag.class);
        this.saveOrUpdate(tag);
    }

    /**
     * 批量删除标签
     * 删除前会检查标签是否已被文章使用
     *
     * @param tagIds 要删除的标签ID列表
     * @throws BizException 当标签下存在文章时抛出异常
     */
    @Override
    public void deleteTag(List<Integer> tagIds) {
        // 检查标签是否已被文章使用
        Integer count = articleTagMapper.selectCount(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getTagId, tagIds));
        // 如果标签已被使用，则抛出异常
        if (count > 0) {
            throw new BizException("删除失败，该标签下存在文章");
        }
        // 批量删除标签
        tagMapper.deleteBatchIds(tagIds);
    }
}

