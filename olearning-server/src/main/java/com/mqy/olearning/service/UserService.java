package com.mqy.olearning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mqy.olearning.common.BusinessException;
import com.mqy.olearning.common.PageResult;
import com.mqy.olearning.dto.LoginDTO;
import com.mqy.olearning.dto.LoginVO;
import com.mqy.olearning.dto.RegisterDTO;
import com.mqy.olearning.entity.SysUser;
import com.mqy.olearning.mapper.SysUserMapper;
import com.mqy.olearning.security.JwtUtil;
import com.mqy.olearning.utils.Md5Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 用户业务：注册、登录、个人信息、管理员用户管理
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;

    /** 注册：教师注册后需管理员启用（status=0），学生直接可用 */
    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在");
        }
        String role = "TEACHER".equals(dto.getRole()) ? "TEACHER" : "STUDENT";
        String salt = Md5Util.randomSalt();

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setSalt(salt);
        user.setPassword(Md5Util.encrypt(dto.getPassword(), salt));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setRole(role);
        user.setStatus("TEACHER".equals(role) ? 0 : 1);
        user.setAvatar(null);
        userMapper.insert(user);
    }

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        // 统一提示，避免暴露"用户是否存在"
        if (user == null || !Md5Util.encrypt(dto.getPassword(), user.getSalt()).equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用或尚未通过管理员审核");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user.getId(), user.getUsername(),
                user.getRealName(), user.getRole(), user.getAvatar());
    }

    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        user.setSalt(null);
        return user;
    }

    public void updateProfile(Long userId, SysUser dto) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!Md5Util.encrypt(oldPassword, user.getSalt()).equals(user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setSalt(Md5Util.randomSalt());
        update.setPassword(Md5Util.encrypt(newPassword, update.getSalt()));
        userMapper.updateById(update);
    }

    // ---------------- 管理员端 ----------------

    public PageResult<SysUser> adminPage(String keyword, String role, long current, long size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                // 关键字命中「用户名 或 真实姓名」需要括起来，否则和后面的 role 条件 AND/OR 优先级会错
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getRealName, keyword))
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        page.getRecords().forEach(u -> {
            u.setPassword(null);
            u.setSalt(null);
        });
        return PageResult.of(page);
    }

    /** 启用/禁用账号；教师注册后的审核开通也走这里 */
    public void updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void resetPassword(Long id) {
        String salt = Md5Util.randomSalt();
        SysUser user = new SysUser();
        user.setId(id);
        user.setSalt(salt);
        user.setPassword(Md5Util.encrypt("123456", salt));
        userMapper.updateById(user);
    }

    public void updateRole(Long id, String role) {
        if (!List.of("STUDENT", "TEACHER", "ADMIN").contains(role)) {
            throw new BusinessException("角色不合法");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole(role);
        userMapper.updateById(user);
    }

    public Map<String, Object> stats() {
        long total = userMapper.selectCount(null);
        long students = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "STUDENT"));
        long teachers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "TEACHER"));
        return Map.of("total", total, "students", students, "teachers", teachers);
    }
}
