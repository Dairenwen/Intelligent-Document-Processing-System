package com.team.docai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.team.docai.entity.Document;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}