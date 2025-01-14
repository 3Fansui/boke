package com.boke.config;

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

import static com.boke.enums.ZoneEnum.SHANGHAI;

@Configuration
public class GlobalZoneConfig {

    @PostConstruct
    public void setGlobalZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(SHANGHAI.getZone()));
    }

}
