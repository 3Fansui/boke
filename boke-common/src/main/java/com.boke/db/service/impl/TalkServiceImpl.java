package com.boke.db.service.impl;

import com.alibaba.fastjson.JSON;
import com.boke.db.mapper.CommentMapper;
import com.boke.db.mapper.TalkMapper;
import com.boke.enums.CommentTypeEnum;
import com.boke.enums.TalkStatusEnum;
import com.boke.model.dto.CommentCountDTO;
import com.boke.model.dto.TalkAdminDTO;
import com.boke.model.dto.TalkDTO;
import com.boke.db.entity.Talk;
import com.boke.exception.BizException;
import com.boke.db.service.TalkService;

import com.boke.util.BeanCopyUtil;
import com.boke.util.CommonUtil;
import com.boke.util.PageUtil;

import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.TalkVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boke.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.boke.enums.TalkStatusEnum.PUBLIC;


/**
 * 说说服务实现类
 * 继承ServiceImpl实现基础的CRUD操作
 * 实现TalkService接口，提供说说相关的业务逻辑处理
 */
@Service
public class TalkServiceImpl extends ServiceImpl<TalkMapper, Talk> implements TalkService {

    /**
     * 说说数据访问接口
     */
    @Autowired
    private TalkMapper talkMapper;

    /**
     * 评论数据访问接口
     */
    @Autowired
    private CommentMapper commentMapper;

    /**
     * 分页获取说说列表
     * 包含评论数统计和图片处理
     *
     * @return 分页结果，包含说说DTO列表和总数
     */
    @Override
    public PageResultDTO<TalkDTO> listTalks() {
        // 查询公开状态的说说数量
        Integer count = talkMapper.selectCount((new LambdaQueryWrapper<Talk>()
                .eq(Talk::getStatus, TalkStatusEnum.PUBLIC.getStatus())));
        // 如果没有数据，返回空结果
        if (count == 0) {
            return new PageResultDTO<>();
        }
        // 分页查询说说列表
        List<TalkDTO> talkDTOs = talkMapper.listTalks(PageUtil.getLimitCurrent(), PageUtil.getSize());
        // 获取所有说说ID
        List<Integer> talkIds = talkDTOs.stream()
                .map(TalkDTO::getId)
                .collect(Collectors.toList());
        // 查询说说评论数并转换为Map
        Map<Integer, Integer> commentCountMap = commentMapper.listCommentCountByTypeAndTopicIds(
                        CommentTypeEnum.TALK.getType(), talkIds)
                .stream()
                .collect(Collectors.toMap(CommentCountDTO::getId, CommentCountDTO::getCommentCount));
        // 处理每条说说的评论数和图片
        talkDTOs.forEach(item -> {
            item.setCommentCount(commentCountMap.get(item.getId()));
            if (Objects.nonNull(item.getImages())) {
                item.setImgs(CommonUtil.castList(JSON.parseObject(item.getImages(), List.class), String.class));
            }
        });
        return new PageResultDTO<>(talkDTOs, count);
    }

    /**
     * 根据ID获取说说详情
     *
     * @param talkId 说说ID
     * @return 说说详情DTO
     * @throws BizException 当说说不存在时抛出异常
     */
    @Override
    public TalkDTO getTalkById(Integer talkId) {
        // 查询说说详情
        TalkDTO talkDTO = talkMapper.getTalkById(talkId);
        if (Objects.isNull(talkDTO)) {
            throw new BizException("说说不存在");
        }
        // 处理说说图片
        if (Objects.nonNull(talkDTO.getImages())) {
            talkDTO.setImgs(CommonUtil.castList(JSON.parseObject(talkDTO.getImages(), List.class), String.class));
        }
        // 查询并设置评论数
        CommentCountDTO commentCountDTO = commentMapper.listCommentCountByTypeAndTopicId(
                CommentTypeEnum.TALK.getType(), talkId);
        if (Objects.nonNull(commentCountDTO)) {
            talkDTO.setCommentCount(commentCountDTO.getCommentCount());
        }
        return talkDTO;
    }

    /**
     * 保存或更新说说
     *
     * @param talkVO 说说视图对象
     */
    @Override
    public void saveOrUpdateTalk(TalkVO talkVO) {
        // 转换对象并设置用户ID
        Talk talk = BeanCopyUtil.copyObject(talkVO, Talk.class);
        talk.setUserId(UserUtil.getUserDetailsDTO().getUserInfoId());
        this.saveOrUpdate(talk);
    }

    /**
     * 批量删除说说
     *
     * @param talkIds 要删除的说说ID列表
     */
    @Override
    public void deleteTalks(List<Integer> talkIds) {
        talkMapper.deleteBatchIds(talkIds);
    }

    /**
     * 后台分页获取说说列表
     *
     * @param conditionVO 查询条件
     * @return 分页结果，包含说说管理DTO列表和总数
     */
    @Override
    public PageResultDTO<TalkAdminDTO> listBackTalks(ConditionVO conditionVO) {
        // 查询说说数量
        Integer count = talkMapper.selectCount(new LambdaQueryWrapper<Talk>()
                .eq(Objects.nonNull(conditionVO.getStatus()),
                        Talk::getStatus,
                        conditionVO.getStatus()));
        // 如果没有数据，返回空结果
        if (count == 0) {
            return new PageResultDTO<>();
        }
        // 分页查询说说列表
        List<TalkAdminDTO> talkDTOs = talkMapper.listTalksAdmin(
                PageUtil.getLimitCurrent(),
                PageUtil.getSize(),
                conditionVO);
        // 处理说说图片
        talkDTOs.forEach(item -> {
            if (Objects.nonNull(item.getImages())) {
                item.setImgs(CommonUtil.castList(JSON.parseObject(item.getImages(), List.class), String.class));
            }
        });
        return new PageResultDTO<>(talkDTOs, count);
    }

    /**
     * 后台获取说说详情
     *
     * @param talkId 说说ID
     * @return 说说管理DTO
     */
    @Override
    public TalkAdminDTO getBackTalkById(Integer talkId) {
        // 查询说说详情
        TalkAdminDTO talkBackDTO = talkMapper.getTalkByIdAdmin(talkId);
        // 处理说说图片
        if (Objects.nonNull(talkBackDTO.getImages())) {
            talkBackDTO.setImgs(CommonUtil.castList(JSON.parseObject(talkBackDTO.getImages(), List.class), String.class));
        }
        return talkBackDTO;
    }
}

