package com.boke.controller;

import com.boke.annotation.OptLog;
import com.boke.model.dto.CategoryAdminDTO;
import com.boke.model.dto.CategoryDTO;
import com.boke.model.dto.CategoryOptionDTO;
import com.boke.service.CategoryService;
import com.boke.model.vo.CategoryVO;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

import static com.boke.constant.OptTypeConstant.*;

@Tag(name =  "分类模块")
@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "获取所有分类")
    @GetMapping("/categories/all")
    public ResultVO<List<CategoryDTO>> listCategories() {
        return ResultVO.ok(categoryService.listCategories());
    }

    @Operation(summary =  "查看后台分类列表")
    @GetMapping("/admin/categories")
    public ResultVO<PageResultDTO<CategoryAdminDTO>> listCategoriesAdmin(ConditionVO conditionVO) {
        return ResultVO.ok(categoryService.listCategoriesAdmin(conditionVO));
    }

    @Operation(summary =  "搜索文章分类")
    @GetMapping("/admin/categories/search")
    public ResultVO<List<CategoryOptionDTO>> listCategoriesAdminBySearch(ConditionVO conditionVO) {
        return ResultVO.ok(categoryService.listCategoriesBySearch(conditionVO));
    }

    @OptLog(optType = DELETE)
    @Operation(summary =  "删除分类")
    @DeleteMapping("/admin/categories")
    public ResultVO<?> deleteCategories(@RequestBody List<Integer> categoryIds) {
        categoryService.deleteCategories(categoryIds);
        return ResultVO.ok();
    }

    @OptLog(optType = SAVE_OR_UPDATE)
    @Operation(summary =  "添加或修改分类")
    @PostMapping("/admin/categories")
    public ResultVO<?> saveOrUpdateCategory(@Valid @RequestBody CategoryVO categoryVO) {
        categoryService.saveOrUpdateCategory(categoryVO);
        return ResultVO.ok();
    }


}
