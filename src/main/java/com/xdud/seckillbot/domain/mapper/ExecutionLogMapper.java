package com.xdud.seckillbot.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdud.seckillbot.domain.entity.ExecutionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExecutionLogMapper extends BaseMapper<ExecutionLog> {
}
