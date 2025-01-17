package com.boke.mapper;

import com.boke.entity.JobLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;

@Mapper
public interface JobLogMapper extends BaseMapper<JobLog> {

    List<String> listJobLogGroups();

}
