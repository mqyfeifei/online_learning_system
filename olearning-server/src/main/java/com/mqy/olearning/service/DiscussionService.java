package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.common.PageResult;
import com.mqy.olearning.entity.DiscussionPost;
import com.mqy.olearning.entity.DiscussionReply;
import com.mqy.olearning.entity.SysUser;
import com.mqy.olearning.mapper.DiscussionPostMapper;
import com.mqy.olearning.mapper.DiscussionReplyMapper;
import com.mqy.olearning.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 讨论区：发帖、回复、点赞、管理员删除
 */
@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionPostMapper postMapper;
    private final DiscussionReplyMapper replyMapper;
    private final SysUserMapper userMapper;

    public PageResult<Map<String, Object>> posts(Long courseId, String keyword, long current, long size) {
        Page<DiscussionPost> page = postMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<DiscussionPost>()
                        .eq(DiscussionPost::getStatus, 1)
                        .eq(courseId != null, DiscussionPost::getCourseId, courseId)
                        .like(StringUtils.hasText(keyword), DiscussionPost::getTitle, keyword)
                        .orderByDesc(DiscussionPost::getId));

        List<Map<String, Object>> list = new ArrayList<>();
        for (DiscussionPost p : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("post", p);
            SysUser author = userMapper.selectById(p.getUserId());
            item.put("authorName", author == null ? "匿名" : author.getRealName());
            Long replyCount = replyMapper.selectCount(new LambdaQueryWrapper<DiscussionReply>()
                    .eq(DiscussionReply::getPostId, p.getId()));
            item.put("replyCount", replyCount);
            list.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    public Map<String, Object> detail(Long postId) {
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
            replyList.add(Map.of(
                    "reply", r,
                    "authorName", user == null ? "匿名" : user.getRealName()
            ));
        }
        SysUser author = userMapper.selectById(post.getUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("post", post);
        data.put("authorName", author == null ? "匿名" : author.getRealName());
        data.put("replies", replyList);
        return data;
    }

    public Long createPost(DiscussionPost post, Long userId) {
        post.setId(null);
        post.setUserId(userId);
        post.setLikeCount(0);
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

    public void like(Long postId) {
        DiscussionPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
        postMapper.updateById(post);
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
}
