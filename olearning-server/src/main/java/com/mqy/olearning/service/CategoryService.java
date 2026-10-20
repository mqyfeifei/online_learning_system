package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.CourseCategory;
import com.mqy.olearning.mapper.CourseCategoryMapper;
import com.mqy.olearning.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程分类：扁平列表 + 树形结构
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CourseCategoryMapper categoryMapper;
    private final CourseMapper courseMapper;

    public List<CourseCategory> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CourseCategory>()
                .orderByAsc(CourseCategory::getParentId)
                .orderByAsc(CourseCategory::getSort));
    }

    /** 组装成两级树，前端 el-tree / el-cascader 直接吃 */
    public List<Map<String, Object>> tree() {
        List<CourseCategory> all = list();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (CourseCategory c : all) {
            if (c.getParentId() == null || c.getParentId() == 0) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", c.getId());
                node.put("name", c.getName());
                node.put("sort", c.getSort());
                List<Map<String, Object>> children = new ArrayList<>();
                for (CourseCategory sub : all) {
                    if (c.getId().equals(sub.getParentId())) {
                        children.add(Map.of("id", sub.getId(), "name", sub.getName(), "sort", sub.getSort()));
                    }
                }
                node.put("children", children);
                roots.add(node);
            }
        }
        return roots;
    }

    public void save(CourseCategory category) {
        if (category.getId() == null) {
            if (category.getParentId() == null) {
                category.setParentId(0L);
            }
            if (category.getSort() == null) {
                category.setSort(0);
            }
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
    }

    public void remove(Long id) {
        Long children = categoryMapper.selectCount(new LambdaQueryWrapper<CourseCategory>()
                .eq(CourseCategory::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("该分类下还有子分类，不能删除");
        }
        Long courses = courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getCategoryId, id));
        if (courses != null && courses > 0) {
            throw new BusinessException("该分类下还有课程，不能删除");
        }
        categoryMapper.deleteById(id);
    }
}
