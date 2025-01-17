package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.FriendLinkAdminDTO;
import com.boke.model.dto.FriendLinkDTO;
import com.boke.model.vo.ResultVO;
import com.boke.service.FriendLinkService;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.FriendLinkVO;
import com.boke.model.dto.PageResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name = "友链模块")
@RestController
public class FriendLinkController {

    @Autowired
    private FriendLinkService friendLinkService;

    @Operation(summary =  "查看友链列表")
    @GetMapping("/links")
    public ResultVO<List<FriendLinkDTO>> listFriendLinks() {
        return ResultVO.ok(friendLinkService.listFriendLinks());
    }

    @Operation(summary =  "查看后台友链列表")
    @GetMapping("/admin/links")
    public ResultVO<PageResultDTO<FriendLinkAdminDTO>> listFriendLinkDTO(ConditionVO conditionVO) {
        return ResultVO.ok(friendLinkService.listFriendLinksAdmin(conditionVO));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @Operation(summary =  "保存或修改友链")
    @PostMapping("/admin/links")
    public ResultVO<?> saveOrUpdateFriendLink(@Valid @RequestBody FriendLinkVO friendLinkVO) {
        friendLinkService.saveOrUpdateFriendLink(friendLinkVO);
        return ResultVO.ok();
    }

    @OptLog(optType = DELETE)
    @Operation(summary =  "删除友链")
    @DeleteMapping("/admin/links")
    public ResultVO<?> deleteFriendLink(@RequestBody List<Integer> linkIdList) {
        friendLinkService.removeByIds(linkIdList);
        return ResultVO.ok();
    }
}
