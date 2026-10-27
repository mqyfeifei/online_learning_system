package com.mqy.olearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mqy.olearning.entity.StudyNote;
import org.apache.ibatis.annotations.Mapper;

/** 学习笔记数据访问层 */
@Mapper
public interface StudyNoteMapper extends BaseMapper<StudyNote> {
}
