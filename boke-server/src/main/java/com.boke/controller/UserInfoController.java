package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.dto.UserInfoDTO;
import com.boke.model.dto.UserOnlineDTO;
import com.boke.service.UserInfoService;
import com.boke.model.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.boke.constant.OptTypeConstant.DELETE;
import static com.boke.constant.OptTypeConstant.UPDATE;

@Tag(name =  "用户信息模块")
@RestController
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @OptLog(optType = UPDATE)
    @Operation(summary = "更新用户信息")
    @PutMapping("/users/info")
    public ResultVO<?> updateUserInfo(@Valid @RequestBody UserInfoVO userInfoVO) {
        userInfoService.updateUserInfo(userInfoVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "更新用户头像")
    @Parameter(name = "file", description = "用户头像", required = true)
    @PostMapping("/users/avatar")
    public ResultVO<String> updateUserAvatar(MultipartFile file) {
        return ResultVO.ok(userInfoService.updateUserAvatar(file));
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "绑定用户邮箱")
    @PutMapping("/users/email")
    public ResultVO<?> saveUserEmail(@Valid @RequestBody EmailVO emailVO) {
        userInfoService.saveUserEmail(emailVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "修改用户的订阅状态")
    @PutMapping("/users/subscribe")
    public ResultVO<?> updateUserSubscribe(@RequestBody SubscribeVO subscribeVO) {
        userInfoService.updateUserSubscribe(subscribeVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary ="修改用户角色")
    @PutMapping("/admin/users/role")
    public ResultVO<?> updateUserRole(@Valid @RequestBody UserRoleVO userRoleVO) {
        userInfoService.updateUserRole(userRoleVO);
        return ResultVO.ok();
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "修改用户禁用状态")
    @PutMapping("/admin/users/disable")
    public ResultVO<?> updateUserDisable(@Valid @RequestBody UserDisableVO userDisableVO) {
        userInfoService.updateUserDisable(userDisableVO);
        return ResultVO.ok();
    }

    @Operation(summary = "查看在线用户")
    @GetMapping("/admin/users/online")
    public ResultVO<PageResultDTO<UserOnlineDTO>> listOnlineUsers(ConditionVO conditionVO) {
        return ResultVO.ok(userInfoService.listOnlineUsers(conditionVO));
    }

    @OptLog(optType = DELETE)
    @Operation(summary =  "下线用户")
    @DeleteMapping("/admin/users/{userInfoId}/online")
    public ResultVO<?> removeOnlineUser(@PathVariable("userInfoId") Integer userInfoId) {
        userInfoService.removeOnlineUser(userInfoId);
        return ResultVO.ok();
    }

    @Operation(summary = "根据id获取用户信息")
    @GetMapping("/users/info/{userInfoId}")
    public ResultVO<UserInfoDTO> getUserInfoById(@PathVariable("userInfoId") Integer userInfoId) {
        return ResultVO.ok(userInfoService.getUserInfoById(userInfoId));
    }

}
