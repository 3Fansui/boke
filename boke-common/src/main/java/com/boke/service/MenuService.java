package com.boke.service;

import com.boke.model.dto.LabelOptionDTO;
import com.boke.model.dto.MenuDTO;
import com.boke.model.dto.UserMenuDTO;
import com.boke.entity.Menu;
import com.boke.model.vo.ConditionVO;
import com.boke.model.vo.IsHiddenVO;
import com.boke.model.vo.MenuVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MenuService extends IService<Menu> {

    List<MenuDTO> listMenus(ConditionVO conditionVO);

    void saveOrUpdateMenu(MenuVO menuVO);

    void updateMenuIsHidden(IsHiddenVO isHiddenVO);

    void deleteMenu(Integer menuId);

    List<LabelOptionDTO> listMenuOptions();

    List<UserMenuDTO> listUserMenus();

}
