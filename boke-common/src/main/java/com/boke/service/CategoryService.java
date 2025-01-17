package com.boke.service;

import com.boke.model.dto.CategoryAdminDTO;
import com.boke.model.dto.CategoryDTO;
import com.boke.model.dto.CategoryOptionDTO;
import com.boke.entity.Category;
import com.boke.model.vo.CategoryVO;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<CategoryDTO> listCategories();

    PageResultDTO<CategoryAdminDTO> listCategoriesAdmin(ConditionVO conditionVO);

    List<CategoryOptionDTO> listCategoriesBySearch(ConditionVO conditionVO);

    void deleteCategories(List<Integer> categoryIds);

    void saveOrUpdateCategory(CategoryVO categoryVO);

}
