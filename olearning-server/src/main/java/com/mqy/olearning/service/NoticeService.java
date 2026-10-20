package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mqy.olearning.entity.SysNotice;
import com.mqy.olearning.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统公告
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final SysNoticeMapper noticeMapper;

    /** 前台：只看已显示，置顶优先 */
    public List<SysNotice> list() {
        return noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .orderByDesc(SysNotice::getTopFlag)
                .orderByDesc(SysNotice::getId));
    }

    /** 管理端：全部 */
    public List<SysNotice> adminList() {
        return noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .orderByDesc(SysNotice::getTopFlag)
                .orderByDesc(SysNotice::getId));
    }

    public void save(SysNotice notice) {
        if (notice.getTopFlag() == null) {
            notice.setTopFlag(0);
        }
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        if (notice.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
    }

    public void updateStatus(Long id, Integer status) {
        SysNotice notice = new SysNotice();
        notice.setId(id);
        notice.setStatus(status);
        noticeMapper.updateById(notice);
    }

    public void remove(Long id) {
        noticeMapper.deleteById(id);
    }
}
