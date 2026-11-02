package com.travelmate.controller;

import com.travelmate.common.BusinessException;
import com.travelmate.common.Result;
import com.travelmate.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 城市图片上传/删除 - 管理员
 */
@RestController
@RequestMapping("/api/v1/city")
@RequiredArgsConstructor
public class CityImageController {

    private final AdminService adminService;

    private void requireAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("currentUserRole");
        if (!"admin".equals(role)) {
            throw BusinessException.forbidden("管理员权限");
        }
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminService.uploadImage(file));
    }

    @DeleteMapping("/{id}/image")
    public Result<Void> deleteImage(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        adminService.clearCityImage(id, adminId, role);
        return Result.success();
    }
}
