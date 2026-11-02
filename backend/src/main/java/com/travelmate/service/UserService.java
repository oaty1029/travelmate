package com.travelmate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.travelmate.common.BusinessException;
import com.travelmate.dto.ChangePasswordDTO;
import com.travelmate.dto.LoginDTO;
import com.travelmate.dto.RegisterDTO;
import com.travelmate.dto.UpdateProfileDTO;
import com.travelmate.entity.User;
import com.travelmate.mapper.UserMapper;
import com.travelmate.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务 - 注册 / 登录 / 资料 / 修改密码
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ImageStorageService imageStorageService;

    /** 注册 */
    public Map<String, Object> register(RegisterDTO dto) {
        String email = dto.getEmail().trim();
        String nickname = dto.getNickname().trim();
        String phone = dto.getPhone() != null ? dto.getPhone().trim() : null;

        ensureUnique("email", email, "邮箱已被注册");
        ensureUnique("nickname", nickname, "昵称已被使用");
        if (phone != null && !phone.isEmpty()) {
            ensureUnique("phone", phone, "手机号已被注册");
        }

        User user = new User();
        user.setEmail(email);
        user.setPhone(phone != null && !phone.isEmpty() ? phone : null);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(nickname);
        user.setRole("user");
        user.setStatus(1);
        userMapper.insert(user);

        String token = jwtUtil.generate(user.getId(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    /** 登录 */
    public Map<String, Object> login(LoginDTO dto) {
        String account = dto.getAccount().trim();
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("email", account)
                .or().eq("phone", account)
                .or().eq("nickname", account);
        User user = userMapper.selectOne(qw);
        if (user == null) {
            throw BusinessException.badRequest("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.forbidden("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("账号或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    /** 获取用户资料 */
    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw BusinessException.notFound("用户不存在");
        return user;
    }

    /** 更新资料 — 只更新传入的非空字段；空串表示清空该字段 */
    @Transactional
    public User updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw BusinessException.notFound("用户不存在");

        boolean clearPhone = false, clearAvatar = false;
        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().trim();
            if (nickname.isEmpty()) throw BusinessException.badRequest("昵称不能为空");
            if (!nickname.equals(user.getNickname())) {
                ensureUnique("nickname", nickname, "昵称已被使用");
                user.setNickname(nickname);
            }
        }
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().trim();
            if (phone.isEmpty()) {
                clearPhone = user.getPhone() != null;
            } else if (!phone.equals(user.getPhone())) {
                ensureUnique("phone", phone, "手机号已被注册");
                user.setPhone(phone);
            }
        }
        if (dto.getAvatarUrl() != null) {
            String avatarUrl = dto.getAvatarUrl().trim();
            if (avatarUrl.isEmpty()) {
                clearAvatar = user.getAvatarUrl() != null;
            } else {
                user.setAvatarUrl(avatarUrl);
            }
        }
        userMapper.updateById(user);
        // updateById 会跳过 null 字段，清空需显式 UPDATE
        if (clearPhone || clearAvatar) {
            LambdaUpdateWrapper<User> uw = Wrappers.lambdaUpdate();
            if (clearPhone) uw.set(User::getPhone, null);
            if (clearAvatar) uw.set(User::getAvatarUrl, null);
            uw.eq(User::getId, userId);
            userMapper.update(null, uw);
        }
        return userMapper.selectById(userId);
    }

    /** 上传头像 — 存图并更新 avatar_url，删除旧本地头像文件 */
    @Transactional
    public User uploadAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) throw BusinessException.notFound("用户不存在");

        String url = imageStorageService.storeImage(file);
        String old = user.getAvatarUrl();
        user.setAvatarUrl(url);
        userMapper.updateById(user);
        if (old != null && !old.equals(url)) {
            imageStorageService.deleteImage(old);
        }
        return userMapper.selectById(userId);
    }

    /** 修改密码 — 校验原密码，成功后前端强制重新登录 */
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw BusinessException.notFound("用户不存在");
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    private void ensureUnique(String column, String value, String message) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq(column, value);
        if (userMapper.selectCount(qw) > 0) {
            throw BusinessException.badRequest(message);
        }
    }
}
