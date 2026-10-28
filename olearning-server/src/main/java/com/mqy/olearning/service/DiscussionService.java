package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.common.PageResult;
import com.mqy.olearning.entity.Course;
import com.mqy.olearning.entity.DiscussionHelpful;
import com.mqy.olearning.entity.DiscussionPost;
import com.mqy.olearning.entity.DiscussionReply;
import com.mqy.olearning.entity.SysUser;
import com.mqy.olearning.mapper.CourseMapper;
import com.mqy.olearning.mapper.DiscussionHelpfulMapper;
import com.mqy.olearning.mapper.DiscussionPostMapper;
import com.mqy.olearning.mapper.DiscussionReplyMapper;
import com.mqy.olearning.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 讨论区：发帖、回复、点赞、「有帮助」
 * <p>帖子都挂在某门课程下（course_id），列表支持按课程标签筛选。
 * 「有帮助」一个人对同一帖只能点一次，用 discussion_helpful 唯一索引兜底。
 */
@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionPostMapper postMapper;
    private final DiscussionReplyMapper replyMapper;
    private final SysUserMapper userMapper;
    private final CourseMapper courseMapper;
    private final DiscussionHelpfulMapper helpfulMapper;

    public PageResult<Map<String, Object>> posts(Long courseId, String keyword, Long userId,
                                                 long current, long size) {
        Page<DiscussionPost> page = postMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<DiscussionPost>()
                        .eq(DiscussionPost::getStatus, 1)
                        .eq(courseId != null, DiscussionPost::getCourseId, courseId)
                        .like(StringUtils.hasText(keyword), DiscussionPost::getTitle, keyword)
                        .orderByDesc(DiscussionPost::getId));

        Set<Long> helpedIds = helpedPostIds(userId, page.getRecords());
        List<Map<String, Object>> list = new ArrayList<>();
        for (DiscussionPost p : page.getRecords()) {
            list.add(toItem(p, helpedIds.contains(p.getId())));
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    /** 课程标签：只列出有帖子的课程，带帖子数，供前端做筛选标签 */
    public List<Map<String, Object>> courseOptions() {
        List<DiscussionPost> all = postMapper.selectList(new LambdaQueryWrapper<DiscussionPost>()
                .eq(DiscussionPost::getStatus, 1));
        Map<Long, Long> countByCourse = all.stream()
                .collect(Collectors.groupingBy(DiscussionPost::getCourseId, Collectors.counting()));
        if (countByCourse.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Course> courses = courseMapper.selectBatchIds(countByCourse.keySet()).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        List<Map<String, Object>> result = new ArrayList<>();
        countByCourse.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    Course c = courses.get(e.getKey());
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("courseId", e.getKey());
                    item.put("courseTitle", c == null ? "已删除课程" : c.getTitle());
                    item.put("count", e.getValue());
                    result.add(item);
                });
        return result;
    }

    public Map<String, Object> detail(Long postId, Long userId) {
        DiscussionPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        List<DiscussionReply> replies = replyMapper.selectList(new LambdaQueryWrapper<DiscussionReply>()
                .eq(DiscussionReply::getPostId, postId)
                .orderByAsc(DiscussionReply::getId));

        List<Map<String, Object>> replyList = new ArrayList<>();
        for (DiscussionReply r : replies) {
            SysUser user = userMapper.selectById(r.getUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reply", r);
            item.put("authorName", authorName(user, r.getUserId()));
            replyList.add(item);
        }
        SysUser author = userMapper.selectById(post.getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("post", post);
        data.put("authorName", authorName(author, post.getUserId()));
        data.put("replies", replyList);
        data.put("courseTitle", courseTitle(post.getCourseId()));
        data.put("helped", userId != null && hasHelped(postId, userId));
        return data;
    }

    public Long createPost(DiscussionPost post, Long userId) {
        if (post.getCourseId() == null) {
            throw new BusinessException("请选择帖子所属课程");
        }
        if (courseMapper.selectById(post.getCourseId()) == null) {
            throw new BusinessException("课程不存在");
        }
        post.setId(null);
        post.setUserId(userId);
        post.setLikeCount(0);
        post.setHelpfulCount(0);
        post.setStatus(1);
        postMapper.insert(post);
        return post.getId();
    }

    public void reply(Long postId, Long userId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("回复内容不能为空");
        }
        DiscussionReply reply = new DiscussionReply();
        reply.setPostId(postId);
        reply.setUserId(userId);
        reply.setContent(content);
        replyMapper.insert(reply);
    }

    @Deprecated
    public void like(Long postId) {
        DiscussionPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
        postMapper.updateById(post);
    }

    /**
     * 点「有帮助」：一个人对同一帖只能点一次。
     * 第二次调用返回 false，前端据此把按钮置为已点亮状态。
     */
    @Transactional
    public boolean helpful(Long postId, Long userId) {
        DiscussionPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Long exists = helpfulMapper.selectCount(new LambdaQueryWrapper<DiscussionHelpful>()
                .eq(DiscussionHelpful::getPostId, postId)
                .eq(DiscussionHelpful::getUserId, userId));
        if (exists != null && exists > 0) {
            return false;
        }
        DiscussionHelpful entity = new DiscussionHelpful();
        entity.setPostId(postId);
        entity.setUserId(userId);
        helpfulMapper.insert(entity);

        DiscussionPost update = new DiscussionPost();
        update.setId(postId);
        update.setHelpfulCount((post.getHelpfulCount() == null ? 0 : post.getHelpfulCount()) + 1);
        postMapper.updateById(update);
        return true;
    }

    /** 作者本人或管理员可删（逻辑删除：status=0） */
    public void remove(Long postId, Long userId, String role) {
        DiscussionPost post = postMapper.selectById(postId);
        if (post == null) {
            return;
        }
        if (!"ADMIN".equals(role) && !post.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的帖子");
        }
        post.setStatus(0);
        postMapper.updateById(post);
    }

    // ---------------- 组装 ----------------

    private Map<String, Object> toItem(DiscussionPost p, boolean helped) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("post", p);
        SysUser author = userMapper.selectById(p.getUserId());
        item.put("authorName", authorName(author, p.getUserId()));
        item.put("courseId", p.getCourseId());
        item.put("courseTitle", courseTitle(p.getCourseId()));
        item.put("helped", helped);
        Long replyCount = replyMapper.selectCount(new LambdaQueryWrapper<DiscussionReply>()
                .eq(DiscussionReply::getPostId, p.getId()));
        item.put("replyCount", replyCount);
        return item;
    }

    private Set<Long> helpedPostIds(Long userId, List<DiscussionPost> posts) {
        if (userId == null || posts.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = posts.stream().map(DiscussionPost::getId).collect(Collectors.toList());
        return helpfulMapper.selectList(new LambdaQueryWrapper<DiscussionHelpful>()
                        .eq(DiscussionHelpful::getUserId, userId)
                        .in(DiscussionHelpful::getPostId, ids))
                .stream().map(DiscussionHelpful::getPostId).collect(Collectors.toSet());
    }

    private boolean hasHelped(Long postId, Long userId) {
        Long count = helpfulMapper.selectCount(new LambdaQueryWrapper<DiscussionHelpful>()
                .eq(DiscussionHelpful::getPostId, postId)
                .eq(DiscussionHelpful::getUserId, userId));
        return count != null && count > 0;
    }

    private String courseTitle(Long courseId) {
        if (courseId == null) {
            return "";
        }
        Course course = courseMapper.selectById(courseId);
        return course == null ? "" : course.getTitle();
    }

    /** 优先显示真实姓名，没有就退回用户名，都不是才显示「匿名」 */
    private String authorName(SysUser user, Long userId) {
        if (user == null) {
            return "用户" + userId;
        }
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getUsername() == null ? "用户" + userId : user.getUsername();
    }
}
