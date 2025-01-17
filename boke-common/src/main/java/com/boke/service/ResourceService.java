package com.boke.service;

import com.boke.model.dto.LabelOptionDTO;
import com.boke.model.dto.ResourceDTO;
import com.boke.entity.Resource;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.ResourceVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ResourceService extends IService<Resource> {

    void importSwagger();

    void saveOrUpdateResource(ResourceVO resourceVO);

    void deleteResource(Integer resourceId);

    List<ResourceDTO> listResources(ConditionVO conditionVO);

    List<LabelOptionDTO> listResourceOption();

}
