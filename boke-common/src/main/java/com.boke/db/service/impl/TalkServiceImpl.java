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


@Service
public class TalkServiceImpl extends ServiceImpl<TalkMapper, Talk> implements TalkService {

    @Autowired
    private TalkMapper talkMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public PageResultDTO<TalkDTO> listTalks() {
        Integer count = talkMapper.selectCount((new LambdaQueryWrapper<Talk>()
                .eq(Talk::getStatus, TalkStatusEnum.PUBLIC.getStatus())));
        if (count == 0) {
            return new PageResultDTO<>();
        }
        List<TalkDTO> talkDTOs = talkMapper.listTalks(PageUtil.getLimitCurrent(), PageUtil.getSize());
        List<Integer> talkIds = talkDTOs.stream()
                .map(TalkDTO::getId)
                .collect(Collectors.toList());
        Map<Integer, Integer> commentCountMap = commentMapper.listCommentCountByTypeAndTopicIds(CommentTypeEnum.TALK.getType(), talkIds)
                .stream()
                .collect(Collectors.toMap(CommentCountDTO::getId, CommentCountDTO::getCommentCount));
        talkDTOs.forEach(item -> {
            item.setCommentCount(commentCountMap.get(item.getId()));
            if (Objects.nonNull(item.getImages())) {
                item.setImgs(CommonUtil.castList(JSON.parseObject(item.getImages(), List.class), String.class));
            }
        });
        return new PageResultDTO<>(talkDTOs, count);
    }

    @Override
    public TalkDTO getTalkById(Integer talkId) {
        TalkDTO talkDTO = talkMapper.getTalkById(talkId);
        if (Objects.isNull(talkDTO)) {
            throw new BizException("说说不存在");
        }
        if (Objects.nonNull(talkDTO.getImages())) {
            talkDTO.setImgs(CommonUtil.castList(JSON.parseObject(talkDTO.getImages(), List.class), String.class));
        }
        CommentCountDTO commentCountDTO = commentMapper.listCommentCountByTypeAndTopicId(CommentTypeEnum.TALK.getType(), talkId);
        if (Objects.nonNull(commentCountDTO)) {
            talkDTO.setCommentCount(commentCountDTO.getCommentCount());
        }
        return talkDTO;
    }

    @Override
    public void saveOrUpdateTalk(TalkVO talkVO) {
        Talk talk = BeanCopyUtil.copyObject(talkVO, Talk.class);
        talk.setUserId(UserUtil.getUserDetailsDTO().getUserInfoId());
        this.saveOrUpdate(talk);
    }

    @Override
    public void deleteTalks(List<Integer> talkIds) {
        talkMapper.deleteBatchIds(talkIds);
    }

    @Override
    public PageResultDTO<TalkAdminDTO> listBackTalks(ConditionVO conditionVO) {
        Integer count = talkMapper.selectCount(new LambdaQueryWrapper<Talk>()
                .eq(Objects.nonNull(conditionVO.getStatus()), Talk::getStatus, conditionVO.getStatus()));
        if (count == 0) {
            return new PageResultDTO<>();
        }
        List<TalkAdminDTO> talkDTOs = talkMapper.listTalksAdmin(PageUtil.getLimitCurrent(), PageUtil.getSize(), conditionVO);
        talkDTOs.forEach(item -> {
            if (Objects.nonNull(item.getImages())) {
                item.setImgs(CommonUtil.castList(JSON.parseObject(item.getImages(), List.class), String.class));
            }
        });
        return new PageResultDTO<>(talkDTOs, count);
    }

    @Override
    public TalkAdminDTO getBackTalkById(Integer talkId) {
        TalkAdminDTO talkBackDTO = talkMapper.getTalkByIdAdmin(talkId);
        if (Objects.nonNull(talkBackDTO.getImages())) {
            talkBackDTO.setImgs(CommonUtil.castList(JSON.parseObject(talkBackDTO.getImages(), List.class), String.class));
        }
        return talkBackDTO;
    }

}

