package com.boke.db.service;

import com.boke.model.dto.PageResultDTO;
import com.boke.model.dto.UserInfoDTO;
import com.boke.model.dto.UserOnlineDTO;
import com.boke.db.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.boke.model.vo.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserInfoService extends IService<UserInfo> {

    void updateUserInfo(UserInfoVO userInfoVO);

    String updateUserAvatar(MultipartFile file);

    void saveUserEmail(EmailVO emailVO);

    void updateUserSubscribe(SubscribeVO subscribeVO);

    void updateUserRole(UserRoleVO userRoleVO);

    void updateUserDisable(UserDisableVO userDisableVO);

    PageResultDTO<UserOnlineDTO> listOnlineUsers(ConditionVO conditionVO);

    void removeOnlineUser(Integer userInfoId);

    UserInfoDTO getUserInfoById(Integer id);

}
