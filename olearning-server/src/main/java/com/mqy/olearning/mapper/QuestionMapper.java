package com.mqy.olearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mqy.olearning.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/** Question 表数据访问层，CRUD 由 MyBatis-Plus 的 BaseMapper 提供 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
