package com.boke.db.service;

import com.boke.model.dto.FriendLinkAdminDTO;
import com.boke.model.dto.FriendLinkDTO;
import com.boke.db.entity.FriendLink;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.FriendLinkVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface FriendLinkService extends IService<FriendLink> {

    List<FriendLinkDTO> listFriendLinks();

    PageResultDTO<FriendLinkAdminDTO> listFriendLinksAdmin(ConditionVO conditionVO);

    void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO);

}
