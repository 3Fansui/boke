package com.boke.controller;


import com.boke.annotation.OptLog;
import com.boke.model.dto.TagAdminDTO;
import com.boke.model.dto.TagDTO;
import com.boke.model.vo.ResultVO;
import com.boke.service.TagService;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name =  "标签模块")
@RestController
public class TagController {


    @Autowired
    private TagService tagService;

    @Operation(summary = "获取所有标签")
    @GetMapping("/tags/all")
    public ResultVO<List<TagDTO>> getAllTags() {
        return ResultVO.ok(tagService.listTags());
    }

    @Operation(summary = "获取前十个标签")
    @GetMapping("/tags/topTen")
    public ResultVO<List<TagDTO>> getTopTenTags() {
        return ResultVO.ok(tagService.listTopTenTags());
    }

    @Operation(summary =  "查询后台标签列表")
    @GetMapping("/admin/tags")
    public ResultVO<PageResultDTO<TagAdminDTO>> listTagsAdmin(ConditionVO conditionVO) {
        return ResultVO.ok(tagService.listTagsAdmin(conditionVO));
    }

    @Operation(summary =  "搜索文章标签")
    @GetMapping("/admin/tags/search")
    public ResultVO<List<TagAdminDTO>> listTagsAdminBySearch(ConditionVO condition) {
        return ResultVO.ok(tagService.listTagsAdminBySearch(condition));
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @Operation(summary =  "添加或修改标签")
    @PostMapping("/admin/tags")
    public ResultVO<?> saveOrUpdateTag(@Valid @RequestBody TagVO tagVO) {
        tagService.saveOrUpdateTag(tagVO);
        return ResultVO.ok();
    }

    @OptLog(optType = DELETE)
    @Operation(summary =  "删除标签")
    @DeleteMapping("/admin/tags")
    public ResultVO<?> deleteTag(@RequestBody List<Integer> tagIdList) {
        tagService.deleteTag(tagIdList);
        return ResultVO.ok();
    }
}
