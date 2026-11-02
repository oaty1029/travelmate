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
 * 通用图片上传 - 管理员（城市/景点共用）
 */
@RestController
@RequestMapping("/api/v1/image")
@RequiredArgsConstructor
public class ImageController {

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
}
