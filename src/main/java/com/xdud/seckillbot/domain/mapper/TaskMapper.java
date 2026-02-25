package com.xdud.seckillbot.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdud.seckillbot.domain.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /** CAS 更新：防止并发状态竞争 */
    @Update("UPDATE task SET status = #{newStatus} WHERE id = #{id} AND status = #{expectedStatus}")
    int casUpdateStatus(@Param("id") Long id,
                        @Param("expectedStatus") String expectedStatus,
                        @Param("newStatus") String newStatus);
}
