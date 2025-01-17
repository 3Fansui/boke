package com.boke.db.service.impl;

import com.boke.constant.CommonConstant;
import com.boke.db.mapper.MenuMapper;
import com.boke.db.mapper.RoleMenuMapper;
import com.boke.model.dto.LabelOptionDTO;
import com.boke.model.dto.MenuDTO;
import com.boke.model.dto.UserMenuDTO;
import com.boke.db.entity.Menu;
import com.boke.db.entity.RoleMenu;
import com.boke.exception.BizException;
import com.boke.db.service.MenuService;
import com.boke.util.BeanCopyUtil;

import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.IsHiddenVO;
import com.boke.model.vo.MenuVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.boke.util.UserUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.boke.constant.CommonConstant.COMPONENT;
import static com.boke.constant.CommonConstant.TRUE;

/**
 * 菜单服务实现类
 * 处理系统菜单的增删改查等相关业务逻辑
 *
 * @author boke
 * @since 1.0
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    /**
     * 菜单数据访问层
     */
    @Autowired
    private MenuMapper menuMapper;

    /**
     * 角色菜单关联数据访问层
     */
    @Autowired
    private RoleMenuMapper roleMenuMapper;


    /**
     * 获取菜单列表
     *
     * @param conditionVO 条件参数，包含关键词搜索
     * @return 菜单列表（包含子菜单）
     */
    @Override
    public List<MenuDTO> listMenus(ConditionVO conditionVO) {
        // 查询所有菜单，如果有关键词则进行模糊匹配
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        Menu::getName,
                        conditionVO.getKeywords()));

        // 获取目录列表（parentId为空的菜单）
        List<Menu> catalogs = listCatalogs(menus);

        // 获取子菜单映射关系
        Map<Integer, List<Menu>> childrenMap = getMenuMap(menus);

        // 转换目录格式，并组装子菜单
        List<MenuDTO> menuDTOs = catalogs.stream().map(item -> {
                    // 转换目录
                    MenuDTO menuDTO = BeanCopyUtil.copyObject(item, MenuDTO.class);
                    // 获取并排序子菜单
                    List<MenuDTO> list = BeanCopyUtil.copyList(childrenMap.get(item.getId()), MenuDTO.class)
                            .stream()
                            .sorted(Comparator.comparing(MenuDTO::getOrderNum))
                            .collect(Collectors.toList());
                    // 设置子菜单
                    menuDTO.setChildren(list);
                    // 处理完的子菜单从Map中移除
                    childrenMap.remove(item.getId());
                    return menuDTO;
                }).sorted(Comparator.comparing(MenuDTO::getOrderNum))
                .collect(Collectors.toList());

        // 处理剩余的子菜单（可能存在没有父级菜单的子菜单）
        if (CollectionUtils.isNotEmpty(childrenMap)) {
            List<Menu> childrenList = new ArrayList<>();
            childrenMap.values().forEach(childrenList::addAll);
            List<MenuDTO> childrenDTOList = childrenList.stream()
                    .map(item -> BeanCopyUtil.copyObject(item, MenuDTO.class))
                    .sorted(Comparator.comparing(MenuDTO::getOrderNum))
                    .collect(Collectors.toList());
            menuDTOs.addAll(childrenDTOList);
        }
        return menuDTOs;
    }

    /**
     * 保存或更新菜单信息
     *
     * @param menuVO 菜单信息VO对象
     */
    @Transactional(rollbackFor = Exception.class)  // 开启事务，任何异常都回滚
    @Override
    public void saveOrUpdateMenu(MenuVO menuVO) {
        // 将VO对象转换为实体对象并保存或更新
        Menu menu = BeanCopyUtil.copyObject(menuVO, Menu.class);
        this.saveOrUpdate(menu);
    }

    /**
     * 更新菜单的隐藏状态
     *
     * @param isHiddenVO 包含菜单ID和是否隐藏状态的VO对象
     */
    @Override
    public void updateMenuIsHidden(IsHiddenVO isHiddenVO) {
        // 将VO对象转换为实体对象并更新
        Menu menu = BeanCopyUtil.copyObject(isHiddenVO, Menu.class);
        menuMapper.updateById(menu);
    }

    /**
     * 删除菜单
     * 删除时会同时删除其子菜单，并检查是否有角色关联
     *
     * @param menuId 需要删除的菜单ID
     * @throws BizException 当菜单下有角色关联时抛出异常
     */
    @Override
    public void deleteMenu(Integer menuId) {
        // 检查是否有角色关联
        Integer count = roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getMenuId, menuId));
        if (count > 0) {
            throw new BizException("菜单下有角色关联");
        }

        // 查询所有子菜单ID
        List<Integer> menuIds = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                        .select(Menu::getId)
                        .eq(Menu::getParentId, menuId))
                .stream()
                //将查询结果转换为流，并提取每个 Menu 对象的 id 属性。
                .map(Menu::getId)
                .collect(Collectors.toList());

        // 将当前菜单ID添加到删除列表
        menuIds.add(menuId);
        // 批量删除菜单及其子菜单
        menuMapper.deleteBatchIds(menuIds);
    }

    /**
     * 获取菜单选项列表
     * 用于前端菜单选择组件
     *
     * @return 菜单选项列表，包含层级关系
     */
    @Override
    public List<LabelOptionDTO> listMenuOptions() {
        // 查询所有菜单的基本信息
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .select(Menu::getId, Menu::getName, Menu::getParentId, Menu::getOrderNum));

        // 获取目录列表
        List<Menu> catalogs = listCatalogs(menus);
        // 获取子菜单映射
        Map<Integer, List<Menu>> childrenMap = getMenuMap(menus);

        // 转换为选项DTO格式
        return catalogs.stream().map(item -> {
            List<LabelOptionDTO> list = new ArrayList<>();
            List<Menu> children = childrenMap.get(item.getId());
            if (CollectionUtils.isNotEmpty(children)) {
                // 转换子菜单为选项格式
                list = children.stream()
                        .sorted(Comparator.comparing(Menu::getOrderNum))
                        .map(menu -> LabelOptionDTO.builder()
                                .id(menu.getId())
                                .label(menu.getName())
                                .build())
                        .collect(Collectors.toList());
            }
            // 构建包含子选项的选项对象
            return LabelOptionDTO.builder()
                    .id(item.getId())
                    .label(item.getName())
                    .children(list)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取当前用户的菜单列表
     * 根据用户ID查询其有权限访问的菜单
     *
     * @return 用户菜单列表，包含层级关系
     */
    @Override
    public List<UserMenuDTO> listUserMenus() {
        // 获取当前用户可访问的所有菜单
        List<Menu> menus = menuMapper.listMenusByUserInfoId(UserUtil.getUserDetailsDTO().getUserInfoId());
        // 获取目录列表
        List<Menu> catalogs = listCatalogs(menus);
        // 获取子菜单映射
        Map<Integer, List<Menu>> childrenMap = getMenuMap(menus);
        // 转换为用户菜单格式
        return convertUserMenuList(catalogs, childrenMap);
    }

    /**
     * 获取目录列表
     * 过滤出parentId为空的菜单项，并按排序号排序
     *
     * @param menus 所有菜单列表
     * @return 目录列表
     */
    private List<Menu> listCatalogs(List<Menu> menus) {
        return menus.stream()
                // 过滤出顶级菜单（目录）
                .filter(item -> Objects.isNull(item.getParentId()))
                // 按排序号升序排序
                .sorted(Comparator.comparing(Menu::getOrderNum))
                .collect(Collectors.toList());
    }

    /**
     * 获取子菜单映射关系
     * 将所有子菜单按照父ID分组
     *
     * @param menus 所有菜单列表
     * @return 父ID到子菜单列表的映射
     */
    private Map<Integer, List<Menu>> getMenuMap(List<Menu> menus) {
        return menus.stream()
                // 过滤出非顶级菜单
                .filter(item -> Objects.nonNull(item.getParentId()))
                // 按父ID分组
                .collect(Collectors.groupingBy(Menu::getParentId));
    }

    /**
     * 转换用户菜单列表
     * 将目录和子菜单转换为前端所需的格式
     *
     * @param catalogList 目录列表
     * @param childrenMap 子菜单映射
     * @return 转换后的用户菜单列表
     */
    private List<UserMenuDTO> convertUserMenuList(List<Menu> catalogList, Map<Integer, List<Menu>> childrenMap) {
        return catalogList.stream().map(item -> {
            // 创建用户菜单DTO对象
            UserMenuDTO userMenuDTO = new UserMenuDTO();
            List<UserMenuDTO> list = new ArrayList<>();

            // 获取当前目录的子菜单
            List<Menu> children = childrenMap.get(item.getId());

            if (CollectionUtils.isNotEmpty(children)) {
                // 如果有子菜单，转换目录信息
                userMenuDTO = BeanCopyUtil.copyObject(item, UserMenuDTO.class);
                // 转换并排序子菜单
                list = children.stream()
                        .sorted(Comparator.comparing(Menu::getOrderNum))
                        .map(menu -> {
                            UserMenuDTO dto = BeanCopyUtil.copyObject(menu, UserMenuDTO.class);
                            // 设置菜单隐藏状态
                            dto.setHidden(menu.getIsHidden().equals(CommonConstant.TRUE));
                            return dto;
                        })
                        .collect(Collectors.toList());
            } else {
                // 如果没有子菜单，设置为叶子节点
                userMenuDTO.setPath(item.getPath());
                userMenuDTO.setComponent(CommonConstant.COMPONENT);
                // 创建默认的子菜单项
                list.add(UserMenuDTO.builder()
                        .path("")
                        .name(item.getName())
                        .icon(item.getIcon())
                        .component(item.getComponent())
                        .build());
            }

            // 设置目录的隐藏状态和子菜单
            userMenuDTO.setHidden(item.getIsHidden().equals(CommonConstant.TRUE));
            userMenuDTO.setChildren(list);
            return userMenuDTO;
        }).collect(Collectors.toList());
    }

}
