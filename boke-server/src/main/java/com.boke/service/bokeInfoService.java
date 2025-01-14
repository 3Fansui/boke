package com.boke.service;

import com.boke.model.dto.AboutDTO;
import com.boke.model.dto.bokeAdminInfoDTO;
import com.boke.model.dto.bokeHomeInfoDTO;
import com.boke.model.dto.WebsiteConfigDTO;
import com.boke.model.vo.AboutVO;
import com.boke.model.vo.WebsiteConfigVO;

public interface bokeInfoService {

    void report();

    bokeHomeInfoDTO getAuroraHomeInfo();

    bokeAdminInfoDTO getAuroraAdminInfo();

    void updateWebsiteConfig(WebsiteConfigVO websiteConfigVO);

    WebsiteConfigDTO getWebsiteConfig();

    void updateAbout(AboutVO aboutVO);

    AboutDTO getAbout();

}
