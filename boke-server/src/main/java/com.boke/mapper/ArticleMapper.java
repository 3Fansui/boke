package com.boke.mapper;

import com.boke.model.dto.ArticleAdminDTO;
import com.boke.model.dto.ArticleCardDTO;
import com.boke.model.dto.ArticleDTO;
import com.boke.model.dto.ArticleStatisticsDTO;
import com.boke.entity.Article;
import com.boke.model.vo.ConditionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ArticleMapper extends BaseMapper<Article> {

    List<ArticleCardDTO> listTopAndFeaturedArticles();

    List<ArticleCardDTO> listArticles(@Param("current") Long current, @Param("size") Long size);

    List<ArticleCardDTO> getArticlesByCategoryId(@Param("current") Long current, @Param("size") Long size, @Param("categoryId") Integer categoryId);

    ArticleDTO getArticleById(@Param("articleId") Integer articleId);

    ArticleCardDTO getPreArticleById(@Param("articleId") Integer articleId);

    ArticleCardDTO getNextArticleById(@Param("articleId") Integer articleId);

    ArticleCardDTO getFirstArticle();

    ArticleCardDTO getLastArticle();

    List<ArticleCardDTO> listArticlesByTagId(@Param("current") Long current, @Param("size") Long size, @Param("tagId") Integer tagId);

    List<ArticleCardDTO> listArchives(@Param("current") Long current, @Param("size") Long size);

    Integer countArticleAdmins(@Param("conditionVO") ConditionVO conditionVO);

    List<ArticleAdminDTO> listArticlesAdmin(@Param("current") Long current, @Param("size") Long size, @Param("conditionVO") ConditionVO conditionVO);

    List<ArticleStatisticsDTO> listArticleStatistics();

}

