package com.boke.service.impl;

import com.boke.model.dto.LabelOptionDTO;
import com.boke.model.dto.ResourceDTO;
import com.boke.entity.Resource;
import com.boke.entity.RoleResource;
import com.boke.exception.BizException;
import com.boke.handler.FilterInvocationSecurityMetadataSourceImpl;
import com.boke.mapper.ResourceMapper;
import com.boke.mapper.RoleResourceMapper;
import com.boke.service.ResourceService;
import com.boke.util.BeanCopyUtil;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.ResourceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.boke.constant.CommonConstant.FALSE;

@Slf4j
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private RoleResourceMapper roleResourceMapper;


   /* @Autowired
    private FilterInvocationSecurityMetadataSourceImpl filterInvocationSecurityMetadataSource;*/
    @Autowired
    private ObjectMapper objectMapper;


    /**
     * 使用jackson解析api的json数据
     */
    @SuppressWarnings("all")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importSwagger() {
        this.remove(null);
        roleResourceMapper.delete(null);

        String json = restTemplate.getForObject("http://localhost:8080/v3/api-docs", String.class);
        if (json == null) {
            System.out.println("未获取到 Swagger 数据");
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode pathsNode = root.path("paths");
            if (pathsNode.isMissingNode() || !pathsNode.isObject()) {
                System.out.println("未找到 paths 信息或 paths 不是一个对象");
                return;
            }

            Map<String, Integer> tagMap = new HashMap<>();
            List<Resource> resourceList = new ArrayList<>();

            Iterator<Map.Entry<String, JsonNode>> pathsIterator = pathsNode.fields();
            while (pathsIterator.hasNext()) {
                Map.Entry<String, JsonNode> pathEntry = pathsIterator.next();
                String pathUrl = pathEntry.getKey();
                JsonNode methodsMap = pathEntry.getValue();

                if (!methodsMap.isObject()) {
                    System.out.println("methodsMap 不是一个对象: " + pathUrl);
                    continue;
                }

                Iterator<Map.Entry<String, JsonNode>> methodsIterator = methodsMap.fields();
                while (methodsIterator.hasNext()) {
                    Map.Entry<String, JsonNode> methodEntry = methodsIterator.next();
                    String requestMethod = methodEntry.getKey().toUpperCase();
                    JsonNode info = methodEntry.getValue();

                    Resource resOne = new Resource();

                    // 提取 tags 数组
                    JsonNode tagsNode = info.path("tags");
                    if (!tagsNode.isArray()) {
                        System.out.println("tags 不是一个数组: " + pathUrl + " " + requestMethod);
                        continue;
                    }

                    List<String> tags = new ArrayList<>();
                    for (JsonNode tagNode : tagsNode) {
                        if (tagNode.isTextual()) {
                            tags.add(tagNode.asText());
                        } else {
                            System.out.println("tag 不是一个字符串: " + tagNode);
                        }
                    }

                    for (String tag : tags) {
                        if (!tagMap.containsKey(tag)) {
                            // 没有的话，插入
                            Resource resource1 = Resource.builder()
                                    .resourceName(tag)
                                    .isAnonymous(FALSE)
                                    .createTime(LocalDateTime.now())
                                    .build();
                            resourceMapper.insert(resource1);
                            tagMap.put(tag, resource1.getId());
                        }
                        // 设置父 id
                        resOne.setParentId(tagMap.get(tag));
                    }

                    // 提取 summary 字符串
                    JsonNode summaryNode = info.path("summary");
                    if (!summaryNode.isTextual()) {
                        System.out.println("summary 不是一个字符串: " + pathUrl + " " + requestMethod);
                        continue;
                    }
                    String summary = summaryNode.asText();

                    resOne.setResourceName(summary);
                    resOne.setUrl(pathUrl);
                    resOne.setRequestMethod(requestMethod);
                    resourceList.add(resOne);
                }
            }

            this.saveBatch(resourceList);
            log.info("接口资源更新完成，数量：" + resourceList.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveOrUpdateResource(ResourceVO resourceVO) {
        Resource resource = BeanCopyUtil.copyObject(resourceVO, Resource.class);
        this.saveOrUpdate(resource);
        //filterInvocationSecurityMetadataSource.clearDataSource();
    }

    @Override
    public void deleteResource(Integer resourceId) {
        Long count = roleResourceMapper.selectCount(new LambdaQueryWrapper<RoleResource>()
                .eq(RoleResource::getResourceId, resourceId));
        if (count > 0) {
            throw new BizException("该资源下存在角色");
        }
        List<Integer> resourceIds = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                        .select(Resource::getId).
                        eq(Resource::getParentId, resourceId))
                .stream()
                .map(Resource::getId)
                .collect(Collectors.toList());
        resourceIds.add(resourceId);
        resourceMapper.deleteBatchIds(resourceIds);
    }

    @Override
    public List<ResourceDTO> listResources(ConditionVO conditionVO) {
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .like(StringUtils.isNotBlank(conditionVO.getKeywords()), Resource::getResourceName, conditionVO.getKeywords()));
        List<Resource> parents = listResourceModule(resources);
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resources);
        List<ResourceDTO> resourceDTOs = parents.stream().map(item -> {
            ResourceDTO resourceDTO = BeanCopyUtil.copyObject(item, ResourceDTO.class);
            List<ResourceDTO> child = BeanCopyUtil.copyList(childrenMap.get(item.getId()), ResourceDTO.class);
            resourceDTO.setChildren(child);
            childrenMap.remove(item.getId());
            return resourceDTO;
        }).collect(Collectors.toList());
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

    @Override
    public List<LabelOptionDTO> listResourceOption() {
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .select(Resource::getId, Resource::getResourceName, Resource::getParentId)
                .eq(Resource::getIsAnonymous, FALSE));
        List<Resource> parents = listResourceModule(resources);
        Map<Integer, List<Resource>> childrenMap = listResourceChildren(resources);
        return parents.stream().map(item -> {
            List<LabelOptionDTO> list = new ArrayList<>();
            List<Resource> children = childrenMap.get(item.getId());
            if (CollectionUtils.isNotEmpty(children)) {
                list = children.stream()
                        .map(resource -> LabelOptionDTO.builder()
                                .id(resource.getId())
                                .label(resource.getResourceName())
                                .build())
                        .collect(Collectors.toList());
            }
            return LabelOptionDTO.builder()
                    .id(item.getId())
                    .label(item.getResourceName())
                    .children(list)
                    .build();
        }).collect(Collectors.toList());
    }


    private List<Resource> listResourceModule(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.isNull(item.getParentId()))
                .collect(Collectors.toList());
    }

    private Map<Integer, List<Resource>> listResourceChildren(List<Resource> resourceList) {
        return resourceList.stream()
                .filter(item -> Objects.nonNull(item.getParentId()))
                .collect(Collectors.groupingBy(Resource::getParentId));
    }

}
