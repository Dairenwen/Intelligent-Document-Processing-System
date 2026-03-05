package com.team.docai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.team.docai.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
