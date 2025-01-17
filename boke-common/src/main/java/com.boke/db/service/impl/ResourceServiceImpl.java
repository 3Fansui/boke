package com.boke.db.service.impl;

import com.boke.constant.CommonConstant;
import com.boke.db.mapper.ResourceMapper;
import com.boke.db.mapper.RoleResourceMapper;
import com.boke.model.dto.LabelOptionDTO;
import com.boke.model.dto.ResourceDTO;
import com.boke.db.entity.Resource;
import com.boke.db.entity.RoleResource;
import com.boke.exception.BizException;
import com.boke.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.boke.db.service.ResourceService;
import com.boke.util.BeanCopyUtil;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.ResourceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.boke.constant.CommonConstant.FALSE;

/**
 * 资源服务实现类
 * 处理系统资源（API接口、权限等）的管理和权限控制
 *
 * @author boke
 * @since 1.0
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

    /**
     * REST请求模板
     * 用于调用Swagger API获取接口信息
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 资源数据访问层接口
     * 处理资源表的基础CRUD操作
     */
    @Autowired
    private ResourceMapper resourceMapper;

    /**
     * 角色资源关联数据访问层接口
     * 处理角色和资源的关联关系
     */
    @Autowired
    private RoleResourceMapper roleResourceMapper;

    /**
     * 安全元数据源实现类
     * 用于刷新系统的权限信息
     */
    @Autowired
    private FilterInvocationSecurityMetadataSourceImpl filterInvocationSecurityMetadataSource;

    /**
     * 从Swagger导入API接口信息
     * 将Swagger文档中的API接口信息转换为系统资源
     * 包括模块（tags）和具体接口（paths）
     */
    @SuppressWarnings("all")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importSwagger() {
        // 清空现有资源和角色资源关联
        this.remove(null);
        roleResourceMapper.delete(null);

        List<Resource> resources = new ArrayList<>();
        // 获取Swagger API文档
        Map<String, Object> data = restTemplate.getForObject("http://localhost:8080/v2/api-docs", Map.class);

        // 处理API模块信息（tags）
        List<Map<String, String>> tagList = (List<Map<String, String>>) data.get("tags");
        tagList.forEach(item -> {
            Resource resource = Resource.builder()
                    .resourceName(item.get("name"))
                    .isAnonymous(CommonConstant.FALSE)
                    .createTime(LocalDateTime.now())
                    .build();
            resources.add(resource);
        });

        // 保存模块资源
        this.saveBatch(resources);

        // 创建模块名称到ID的映射
        Map<String, Integer> permissionMap = resources.stream()
                .collect(Collectors.toMap(Resource::getResourceName, Resource::getId));
        resources.clear();

        // 处理具体API接口信息（paths）
        Map<String, Map<String, Map<String, Object>>> path =
                (Map<String, Map<String, Map<String, Object>>>) data.get("paths");
        path.forEach((url, value) -> value.forEach((requestMethod, info) -> {
            String permissionName = info.get("summary").toString();
            List<String> tag = (List<String>) info.get("tags");
            Integer parentId = permissionMap.get(tag.get(0));

            // 构建接口资源对象
            Resource resource = Resource.builder()
                    .resourceName(permissionName)
                    .url(url.replaceAll("\\{[^}]*\\}", "*"))  // 将路径参数替换为通配符
                    .parentId(parentId)
                    .requestMethod(requestMethod.toUpperCase())
                    .isAnonymous(CommonConstant.FALSE)
                    .createTime(LocalDateTime.now())
                    .build();
            resources.add(resource);
        }));

        // 保存接口资源
        this.saveBatch(resources);
    }

    /**
     * 保存或更新资源信息
     * 更新完成后会清空权限缓存
     *
     * @param resourceVO 资源信息VO对象
     */
    @Override
    public void saveOrUpdateResource(ResourceVO resourceVO) {
        // 将VO对象转换为实体并保存或更新
        Resource resource = BeanCopyUtil.copyObject(resourceVO, Resource.class);
        this.saveOrUpdate(resource);
        // 清空权限缓存，强制刷新权限信息
        filterInvocationSecurityMetadataSource.clearDataSource();
    }

    /**
     * 删除资源
     * 删除资源时会同时删除其子资源，并检查是否有角色关联
     *
     * @param resourceId 资源ID
     * @throws BizException 当资源存在角色关联时抛出异常
     */
    @Override
    public void deleteResource(Integer resourceId) {
        // 检查是否存在角色关联
        Integer count = roleResourceMapper.selectCount(new LambdaQueryWrapper<RoleResource>()
                .eq(RoleResource::getResourceId, resourceId));
        if (count > 0) {
            throw new BizException("该资源下存在角色");
        }

        // 查询并收集所有子资源ID
        List<Integer> resourceIds = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                        .select(Resource::getId)
                        .eq(Resource::getParentId, resourceId))
                .stream()
                .map(Resource::getId)
                .collect(Collectors.toList());
        // 添加当前资源ID
        resourceIds.add(resourceId);
        // 批量删除资源
        resourceMapper.deleteBatchIds(resourceIds);
    }

    /**
     * 查询资源列表
     * 支持按资源名称关键字搜索，并构建父子层级关系
     *
     * @param conditionVO 查询条件
     * @return 资源列表，包含层级关系
     */
    @Override
    public List<ResourceDTO> listResources(ConditionVO conditionVO) {
        // 查询所有符合条件的资源
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()),
                        Resource::getResourceName,
                        conditionVO.getKeywords()));

        // 获取父级资源
        List<Resource> parents = listResourceModule(resources);
        // 获取子资源映射
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resources);

        // 构建资源树形结构
        List<ResourceDTO> resourceDTOs = parents.stream().map(item -> {
            ResourceDTO resourceDTO = BeanCopyUtil.copyObject(item, ResourceDTO.class);
            List<ResourceDTO> child = BeanCopyUtil.copyList(childrenMap.get(item.getId()), ResourceDTO.class);
            resourceDTO.setChildren(child);
            childrenMap.remove(item.getId());
            return resourceDTO;
        }).collect(Collectors.toList());

        // 处理剩余的子资源（可能存在没有父级的子资源）
        if (CollectionUtils.isNotEmpty(childrenMap)) {
            List<Resource> childrenList = new ArrayList<>();
            childrenMap.values().forEach(childrenList::addAll);
            List<ResourceDTO> childrenDTOs = childrenList.stream()
                    .map(item -> BeanCopyUtil.copyObject(item, ResourceDTO.class))
                    .collect(Collectors.toList());
            resourceDTOs.addAll(childrenDTOs);
        }
        return resourceDTOs;
    }

    /**
     * 获取资源选项列表
     * 将资源转换为前端选择组件需要的格式，包含父子层级关系
     *
     * @return 资源选项列表，包含标签和值的树形结构
     */
    @Override
    public List<LabelOptionDTO> listResourceOption() {
        // 查询所有非匿名访问的资源
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .select(Resource::getId, Resource::getResourceName, Resource::getParentId)
                .eq(Resource::getIsAnonymous, CommonConstant.FALSE));

        // 获取父级资源
        List<Resource> parents = listResourceModule(resources);
        // 获取子资源映射
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resources);

        // 构建选项树形结构
        return parents.stream().map(item -> {
            List<LabelOptionDTO> list = new ArrayList<>();
            // 处理子资源
            List<Resource> children = childrenMap.get(item.getId());
            if (CollectionUtils.isNotEmpty(children)) {
                list = children.stream()
                        .map(resource -> LabelOptionDTO.builder()
                                .id(resource.getId())
                                .label(resource.getResourceName())
                                .build())
                        .collect(Collectors.toList());
            }
            // 构建父级选项
            return LabelOptionDTO.builder()
                    .id(item.getId())
                    .label(item.getResourceName())
                    .children(list)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取资源模块列表
     * 筛选出所有父级资源（没有parentId的资源）
     *
     * @param resourceList 资源列表
     * @return 父级资源列表
     */
    private List<Resource> listResourceModule(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.isNull(item.getParentId()))
                .collect(Collectors.toList());
    }

    /**
     * 获取子资源映射
     * 将所有子资源按照父ID分组
     *
     * @param resourceList 资源列表
     * @return 父ID到子资源列表的映射
     */
    private Map<Integer, List<Resource>> listResourceChildren(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.nonNull(item.getParentId()))
                .collect(Collectors.groupingBy(Resource::getParentId));
    }

}
