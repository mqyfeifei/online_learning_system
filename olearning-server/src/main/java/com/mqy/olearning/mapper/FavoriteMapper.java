package com.mqy.olearning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mqy.olearning.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/** 收藏表数据访问层 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
