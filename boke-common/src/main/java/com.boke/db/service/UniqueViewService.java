package com.boke.db.service;

import com.boke.model.dto.UniqueViewDTO;
import com.boke.db.entity.UniqueView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UniqueViewService extends IService<UniqueView> {

    List<UniqueViewDTO> listUniqueViews();

}
