package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.dto.ChangePasswordDTO;
import com.travelmate.dto.UpdateProfileDTO;
import com.travelmate.entity.User;
import com.travelmate.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器 - 个人中心
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping({"/profile", "/me"})
    public Result<User> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody UpdateProfileDTO dto,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(userService.updateProfile(userId, dto));
    }

    @PostMapping("/avatar")
    public Result<User> uploadAvatar(@RequestParam("file") MultipartFile file,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(userService.uploadAvatar(userId, file));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto,
                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        userService.changePassword(userId, dto);
        return Result.success();
    }
}
