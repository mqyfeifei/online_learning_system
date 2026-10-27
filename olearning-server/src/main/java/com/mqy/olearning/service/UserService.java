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

    // ---------------- 批量导入 ----------------

    /** 导入模板表头，前端下载模板与后端解析共用同一套字段 */
    public static final String IMPORT_HEADER = "用户名,真实姓名,角色,手机号,初始密码";

    /**
     * 批量导入用户：逐行校验，任何一行不合法只记录该行失败原因，不影响其它行。
     * <p>列顺序：用户名,真实姓名,角色(STUDENT/TEACHER),手机号(可空),初始密码(可空，默认 123456)
     */
    public Map<String, Object> importUsers(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new BusinessException("导入内容为空");
        }
        List<Map<String, Object>> success = new java.util.ArrayList<>();
        List<Map<String, Object>> failed = new java.util.ArrayList<>();
        String[] lines = rawText.replace("\uFEFF", "").split("\\R");
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            String text = line.trim();
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            // 表头行跳过
            if (lineNo == 1 && (text.contains("用户名") || text.toLowerCase().startsWith("username"))) {
                continue;
            }
            String[] cols = text.split("[,，\\t]");
            String username = cell(cols, 0);
            String realName = cell(cols, 1);
            String role = cell(cols, 2).toUpperCase();
            String phone = cell(cols, 3);
            String password = cell(cols, 4);
            if (password.isEmpty()) {
                password = "123456";
            }

            String error = validateImport(username, realName, role, phone);
            if (error != null) {
                failed.add(importFail(lineNo, username, error));
                continue;
            }
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, username));
            if (exists != null && exists > 0) {
                failed.add(importFail(lineNo, username, "用户名已存在"));
                continue;
            }

            String salt = Md5Util.randomSalt();
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setRealName(realName);
            user.setPhone(phone.isEmpty() ? null : phone);
            user.setRole(role);
            user.setSalt(salt);
            user.setPassword(Md5Util.encrypt(password, salt));
            // 教师账号仍需管理员启用，学生直接可用，与注册逻辑保持一致
            user.setStatus("TEACHER".equals(role) ? 0 : 1);
            try {
                userMapper.insert(user);
            } catch (Exception e) {
                failed.add(importFail(lineNo, username, "写入失败，可能用户名重复"));
                continue;
            }
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("line", lineNo);
            item.put("username", username);
            item.put("realName", realName);
            item.put("role", role);
            success.add(item);
        }

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("success", success);
        data.put("failed", failed);
        data.put("successCount", success.size());
        data.put("failedCount", failed.size());
        return data;
    }

    /** 导入模板（UTF-8 BOM，Excel 打开不乱码） */
    public byte[] importTemplate() {
        String sample = IMPORT_HEADER + "\r\n"
                + "student3,张三,STUDENT,13800000005,123456\r\n"
                + "student4,李四,STUDENT,,\r\n"
                + "teacher3,王老师,TEACHER,13800000006,123456\r\n";
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = sample.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        return result;
    }

    private String validateImport(String username, String realName, String role, String phone) {
        if (username.isEmpty()) {
            return "用户名不能为空";
        }
        if (!username.matches("^[A-Za-z0-9_]{4,20}$")) {
            return "用户名需为 4-20 位字母、数字或下划线";
        }
        if (realName.isEmpty()) {
            return "真实姓名不能为空";
        }
        if (realName.length() > 50) {
            return "真实姓名不能超过 50 个字";
        }
        if (!List.of("STUDENT", "TEACHER").contains(role)) {
            return "角色只能是 STUDENT 或 TEACHER";
        }
        if (!phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            return "手机号格式不正确";
        }
        return null;
    }

    private String cell(String[] cols, int index) {
        return index < cols.length && cols[index] != null ? cols[index].trim() : "";
    }

    private Map<String, Object> importFail(int line, String username, String reason) {
        Map<String, Object> item = new java.util.LinkedHashMap<>();
        item.put("line", line);
        item.put("username", username);
        item.put("reason", reason);
        return item;
    }

    public Map<String, Object> stats() {
        long total = userMapper.selectCount(null);
        long students = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "STUDENT"));
        long teachers = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "TEACHER"));
        return Map.of("total", total, "students", students, "teachers", teachers);
    }
}
