package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.TalkAdminDTO;
import com.boke.model.dto.TalkDTO;
import com.boke.enums.FilePathEnum;
import com.boke.model.vo.ResultVO;
import com.boke.service.TalkService;
import com.boke.strategy.context.UploadStrategyContext;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.TalkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name = "说说模块")
@RestController
public class TalkController {

    @Autowired
    private TalkService talkService;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @Operation(summary = "查看说说列表")
    @GetMapping("/talks")
    public ResultVO<PageResultDTO<TalkDTO>> listTalks() {
        return ResultVO.ok(talkService.listTalks());
    }

    @Operation(summary = "根据id查看说说")
    @Parameter(name = "talkId", description = "说说id", required = true)
    @GetMapping("/talks/{talkId}")
    public ResultVO<TalkDTO> getTalkById(@PathVariable("talkId") Integer talkId) {
        return ResultVO.ok(talkService.getTalkById(talkId));
    }

    @OptLog(optType = UPLOAD)
    @Operation(summary = "上传说说图片")
    @Parameter(name = "file", description = "说说图片", required = true)
    @PostMapping("/admin/talks/images")
    public ResultVO<String> saveTalkImages(MultipartFile file) {
        return ResultVO.ok(uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.TALK.getPath()));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @Operation(summary = "保存或修改说说")
    @PostMapping("/admin/talks")
    public ResultVO<?> saveOrUpdateTalk(@Valid @RequestBody TalkVO talkVO) {
        talkService.saveOrUpdateTalk(talkVO);
        return ResultVO.ok();
    }

    @OptLog(optType = DELETE)
    @Operation(summary = "删除说说")
    @DeleteMapping("/admin/talks")
    public ResultVO<?> deleteTalks(@RequestBody List<Integer> talkIds) {
        talkService.deleteTalks(talkIds);
        return ResultVO.ok();
    }

    @Operation(summary = "查看后台说说")
    @GetMapping("/admin/talks")
    public ResultVO<PageResultDTO<TalkAdminDTO>> listBackTalks(ConditionVO conditionVO) {
        return ResultVO.ok(talkService.listBackTalks(conditionVO));
    }

    @Operation(summary = "根据id查看后台说说")
    @Parameter(name = "talkId", description = "说说id", required = true)
    @GetMapping("/admin/talks/{talkId}")
    public ResultVO<TalkAdminDTO> getBackTalkById(@PathVariable("talkId") Integer talkId) {
        return ResultVO.ok(talkService.getBackTalkById(talkId));
    }

}
