package com.boke.db.service.impl;

import com.boke.constant.CommonConstant;
import com.boke.db.mapper.RoleMapper;
import com.boke.db.mapper.UserRoleMapper;
import com.boke.db.service.RoleService;
import com.boke.model.dto.RoleDTO;
import com.boke.model.dto.UserRoleDTO;
import com.boke.db.entity.Role;
import com.boke.db.entity.RoleMenu;
import com.boke.db.entity.RoleResource;
import com.boke.db.entity.UserRole;
import com.boke.exception.BizException;
import com.boke.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.boke.db.service.RoleMenuService;
import com.boke.db.service.RoleResourceService;
import com.boke.util.BeanCopyUtil;
import com.boke.util.PageUtil;
import com.boke.model.vo.ConditionVO;
import com.boke.model.dto.PageResultDTO;
import com.boke.model.vo.RoleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 * 继承ServiceImpl实现基础的CRUD操作
 * 实现RoleService接口，提供角色相关的业务逻辑处理
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    /**
     * 角色数据访问接口，用于操作角色表
     */
    @Autowired
    private RoleMapper roleMapper;

    /**
     * 用户角色关联数据访问接口，用于操作用户-角色关联表
     */
    @Autowired
    private UserRoleMapper userRoleMapper;

    /**
     * 角色资源服务接口，用于管理角色-资源关联关系
     */
    @Autowired
    private RoleResourceService roleResourceService;

    /**
     * 角色菜单服务接口，用于管理角色-菜单关联关系
     */
    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * Spring Security 权限元数据源实现类
     * 用于刷新权限缓存
     */
    @Autowired
    private FilterInvocationSecurityMetadataSourceImpl filterInvocationSecurityMetadataSource;

    /**
     * 获取用户角色列表
     * 只查询角色的ID和名称字段
     *
     * @return 返回用户角色DTO列表
     */
    @Override
    public List<UserRoleDTO> listUserRoles() {
        // 创建查询条件，只选择id和角色名称字段
        List<Role> roleList = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .select(Role::getId, Role::getRoleName));
        // 将Role实体列表转换为UserRoleDTO列表
        return BeanCopyUtil.copyList(roleList, UserRoleDTO.class);
    }

    /**
     * 分页查询角色列表
     * 支持按关键词模糊搜索角色名称
     * 使用异步方式统计总数
     *
     * @param conditionVO 查询条件，包含关键词等参数
     * @return 分页结果，包含角色列表和总数
     */
    @SneakyThrows
    @Override
    public PageResultDTO<RoleDTO> listRoles(ConditionVO conditionVO) {
        // 构建查询条件
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<Role>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        Role::getRoleName,
                        conditionVO.getKeywords());

        // 异步查询总数
        CompletableFuture<Integer> asyncCount = CompletableFuture.supplyAsync(() ->
                roleMapper.selectCount(queryWrapper));

        // 查询角色列表
        List<RoleDTO> roleDTOs = roleMapper.listRoles(
                PageUtil.getLimitCurrent(),
                PageUtil.getSize(),
                conditionVO);

        // 返回分页结果
        return new PageResultDTO<>(roleDTOs, asyncCount.get());
    }

    /**
     * 保存或更新角色信息
     * 包括角色基本信息、角色-资源关联、角色-菜单关联的处理
     *
     * @param roleVO 角色信息，包含基本信息及关联的资源ID和菜单ID
     * @throws BizException 当角色名称已存在时抛出异常
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateRole(RoleVO roleVO) {
        // 检查角色名称是否已存在
        Role roleCheck = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .select(Role::getId)
                .eq(Role::getRoleName, roleVO.getRoleName()));

        // 如果角色名存在且不是当前编辑的角色，则抛出异常
        if (Objects.nonNull(roleCheck) && !(roleCheck.getId().equals(roleVO.getId()))) {
            throw new BizException("该角色存在");
        }

        // 构建角色实体并保存
        Role role = Role.builder()
                .id(roleVO.getId())
                .roleName(roleVO.getRoleName())
                .isDisable(CommonConstant.FALSE)
                .build();
        this.saveOrUpdate(role);

        // 处理角色-资源关联
        if (Objects.nonNull(roleVO.getResourceIds())) {
            // 如果是更新操作，先删除原有关联
            if (Objects.nonNull(roleVO.getId())) {
                roleResourceService.remove(new LambdaQueryWrapper<RoleResource>()
                        .eq(RoleResource::getRoleId, roleVO.getId()));
            }
            // 构建新的角色-资源关联关系并批量保存
            List<RoleResource> roleResourceList = roleVO.getResourceIds().stream()
                    .map(resourceId -> RoleResource.builder()
                            .roleId(role.getId())
                            .resourceId(resourceId)
                            .build())
                    .collect(Collectors.toList());
            roleResourceService.saveBatch(roleResourceList);
            // 清除权限缓存
            filterInvocationSecurityMetadataSource.clearDataSource();
        }

        // 处理角色-菜单关联
        if (Objects.nonNull(roleVO.getMenuIds())) {
            // 如果是更新操作，先删除原有关联
            if (Objects.nonNull(roleVO.getId())) {
                roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>()
                        .eq(RoleMenu::getRoleId, roleVO.getId()));
            }
            // 构建新的角色-菜单关联关系并批量保存
            List<RoleMenu> roleMenuList = roleVO.getMenuIds().stream()
                    .map(menuId -> RoleMenu.builder()
                            .roleId(role.getId())
                            .menuId(menuId)
                            .build())
                    .collect(Collectors.toList());
            roleMenuService.saveBatch(roleMenuList);
        }
    }

    /**
     * 批量删除角色
     * 删除前会检查角色是否已分配给用户
     *
     * @param roleIdList 要删除的角色ID列表
     * @throws BizException 当角色下存在用户时抛出异常
     */
    @Override
    public void deleteRoles(List<Integer> roleIdList) {
        // 检查角色是否已分配给用户
        Integer count = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .in(UserRole::getRoleId, roleIdList));
        // 如果角色已被分配，则抛出异常
        if (count > 0) {
            throw new BizException("该角色下存在用户");
        }
        // 批量删除角色
        roleMapper.deleteBatchIds(roleIdList);
    }
}
