package com.boke.db.service.impl;

import com.boke.db.mapper.FriendLinkMapper;
import com.boke.model.dto.FriendLinkAdminDTO;
import com.boke.model.dto.FriendLinkDTO;
import com.boke.db.entity.FriendLink;
import com.boke.db.service.FriendLinkService;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.FriendLinkVO;
import com.boke.model.dto.PageResultDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 友情链接服务实现类
 *
 * @author boke
 * @since 1.0
 */
@Service
public class FriendLinkServiceImpl extends ServiceImpl<FriendLinkMapper, FriendLink> implements FriendLinkService {

    /**
     * 友情链接数据访问层
     */
    @Autowired
    private FriendLinkMapper friendLinkMapper;

    /**
     * 查询友情链接列表
     *
     * @return 友情链接列表
     */
    @Override
    public List<FriendLinkDTO> listFriendLinks() {
        // 查询友情链接列表
        List<FriendLink> friendLinks = friendLinkMapper.selectList(null);
        // 转换为DTO对象返回
        return BeanCopyUtil.copyList(friendLinks, FriendLinkDTO.class);
    }

    /**
     * 查询后台友情链接列表
     *
     * @param conditionVO 条件
     * @return 后台友情链接列表
     */
    @Override
    public PageResultDTO<FriendLinkAdminDTO> listFriendLinksAdmin(ConditionVO conditionVO) {
        // 分页查询友情链接列表
        Page<FriendLink> page = new Page<>(PageUtil.getCurrent(), PageUtil.getSize());
        // 条件查询
        Page<FriendLink> friendLinkPage = friendLinkMapper.selectPage(page, new LambdaQueryWrapper<FriendLink>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), FriendLink::getLinkName, conditionVO.getKeywords()));
        // 转换DTO列表
        List<FriendLinkAdminDTO> friendLinkBackDTOs = BeanCopyUtil.copyList(friendLinkPage.getRecords(), FriendLinkAdminDTO.class);
        // 返回分页结果
        return new PageResultDTO<>(friendLinkBackDTOs, (int) friendLinkPage.getTotal());
    }

    /**
     * 保存或更新友情链接
     *
     * @param friendLinkVO 友情链接
     */
    @Override
    public void saveOrUpdateFriendLink(FriendLinkVO friendLinkVO) {
        // 转换友情链接对象
        FriendLink friendLink = BeanCopyUtil.copyObject(friendLinkVO, FriendLink.class);
        // 保存或更新友情链接
        this.saveOrUpdate(friendLink);
    }

}
