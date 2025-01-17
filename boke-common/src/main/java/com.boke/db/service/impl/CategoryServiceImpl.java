package com.boke.db.service.impl;

import com.boke.db.mapper.ArticleMapper;
import com.boke.db.mapper.CategoryMapper;
import com.boke.model.dto.CategoryAdminDTO;
import com.boke.model.dto.CategoryDTO;
import com.boke.model.dto.CategoryOptionDTO;
import com.boke.db.entity.Article;
import com.boke.db.entity.Category;
import com.boke.exception.BizException;
import com.boke.db.service.CategoryService;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.boke.model.vo.CategoryVO;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 文章分类服务实现类
 *
 * 提供以下功能：
 * 1. 分类列表查询（前台展示）
 * 2. 分类管理（后台管理）
 * 3. 分类搜索
 * 4. 分类的添加、修改、删除
 *
 * @author boke
 * @since 1.0.0
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleMapper articleMapper;

    /**
     * 获取所有分类列表（前台展示使用）
     *
     * @return List<CategoryDTO> 分类列表
     */
    @Override
    public List<CategoryDTO> listCategories() {
        return categoryMapper.listCategories();
    }

    /**
     * 查看后台分类列表（分页）
     * 支持根据分类名称关键字搜索
     *
     * @param conditionVO 查询条件
     * @return PageResultDTO<CategoryAdminDTO> 分页结果
     */
    @SneakyThrows
    @Override
    public PageResultDTO<CategoryAdminDTO> listCategoriesAdmin(ConditionVO conditionVO) {
        // 统计符合条件的分类数量
        Integer count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                // 只有当 keywords 不为空时才添加 LIKE 条件
                //第一个参数是 boolean 类型：决定是否将该条件加入到查询中
                //第二个参数是字段：指定要查询的数据库字段
                //第三个参数是查询值：要匹配的值
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        Category::getCategoryName,
                        conditionVO.getKeywords()));

        // 判断是否有数据
        if (count == 0) {
            return new PageResultDTO<>();
        }

        // 查询分类列表
        List<CategoryAdminDTO> categoryList = categoryMapper.listCategoriesAdmin(
                PageUtil.getLimitCurrent(),
                PageUtil.getSize(),
                conditionVO);

        // 返回分页结果
        return new PageResultDTO<>(categoryList, count);
    }

    /**
     * 搜索文章分类（后台）
     * 根据关键字搜索分类，用于文章编辑时选择分类
     *
     * @param conditionVO 条件VO，包含搜索关键字
     * @return List<CategoryOptionDTO> 分类选项列表
     */
    @SneakyThrows
    @Override
    public List<CategoryOptionDTO> listCategoriesBySearch(ConditionVO conditionVO) {
        // 根据关键字查询分类列表
        List<Category> categoryList = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        Category::getCategoryName,
                        conditionVO.getKeywords())
                .orderByDesc(Category::getId));

        // 转换为DTO返回
        return BeanCopyUtil.copyList(categoryList, CategoryOptionDTO.class);
    }

    /**
     * 删除分类
     * 删除前会检查分类下是否存在文章
     *
     * @param categoryIds 要删除的分类ID列表
     * @throws BizException 当分类下存在文章时抛出异常
     */
    @Override
    public void deleteCategories(List<Integer> categoryIds) {
        // 检查分类下是否有文章
        Integer count = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .in(Article::getCategoryId, categoryIds));

        // 如果分类下有文章，则不允许删除
        if (count > 0) {
            throw new BizException("删除失败，该分类下存在文章");
        }

        // 批量删除分类
        categoryMapper.deleteBatchIds(categoryIds);
    }

    /**
     * 添加或修改分类
     * 保存前会检查分类名称是否已存在
     *
     * @param categoryVO 分类信息VO
     * @throws BizException 当分类名称已存在时抛出异常
     */
    @Override
    public void saveOrUpdateCategory(CategoryVO categoryVO) {
        // 检查分类名称是否已存在
        Category existCategory = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getId)
                .eq(Category::getCategoryName, categoryVO.getCategoryName()));

        // 如果分类名已存在且不是当前正在修改的分类，则抛出异常
        if (Objects.nonNull(existCategory) && !existCategory.getId().equals(categoryVO.getId())) {
            throw new BizException("分类名已存在");
        }

        // 构建分类实体并保存或更新
        Category category = Category.builder()
                .id(categoryVO.getId())
                .categoryName(categoryVO.getCategoryName())
                .build();
        this.saveOrUpdate(category);
    }
}