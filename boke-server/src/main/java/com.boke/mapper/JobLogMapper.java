package com.boke.mapper;

import com.boke.entity.JobLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobLogMapper extends BaseMapper<JobLog> {

    List<String> listJobLogGroups();

}
