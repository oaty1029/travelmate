package com.travelmate.controller;

import com.travelmate.common.BusinessException;
import com.travelmate.common.PageResult;
import com.travelmate.common.Result;
import com.travelmate.dto.AttractionDTO;
import com.travelmate.dto.CityDTO;
import com.travelmate.dto.PageQuery;
import com.travelmate.dto.PostBatchDTO;
import com.travelmate.entity.AiConfig;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.City;
import com.travelmate.entity.ForumPost;
import com.travelmate.entity.Itinerary;
import com.travelmate.entity.OperationLog;
import com.travelmate.entity.User;
import com.travelmate.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器 - 每个方法手动校验 role（admin）
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    private void requireAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("currentUserRole");
        if (!"admin".equals(role)) {
            throw BusinessException.forbidden("管理员权限");
        }
    }

    // ========== 仪表盘 ==========

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminService.getDashboardStats());
    }

    // ========== AI 配置 ==========

    @GetMapping("/ai-config")
    public Result<AiConfig> getAiConfig(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminService.getAiConfig());
    }

    @PutMapping("/ai-config")
    public Result<AiConfig> updateAiConfig(@RequestBody AiConfig dto, HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        return Result.success(adminService.updateAiConfig(dto, adminId, role));
    }

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public Result<PageResult<User>> getUsers(@RequestParam(required = false) String nickname,
                                             @Valid PageQuery query,
                                             HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(PageResult.of(
                adminService.getUsers(nickname, query.getPage(), query.getPageSize())));
    }

    @GetMapping("/users/{id}")
    public Result<User> getUser(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminService.getUserById(id));
    }

    @PutMapping("/users/{id}/status")
    public Result<User> updateUserStatus(@PathVariable Long id,
                                         @RequestBody Map<String, Integer> body,
                                         HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        return Result.success(adminService.updateUserStatus(id, body.get("status"), adminId, role));
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        adminService.deleteUser(id, adminId, role);
        return Result.success();
    }

    // ========== 城市管理 ==========

    @GetMapping("/cities")
    public Result<PageResult<City>> getCities(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long provinceId,
                                              @Valid PageQuery query,
                                              HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(PageResult.of(
                adminService.getCities(keyword, provinceId, query.getPage(), query.getPageSize())));
    }

    @PostMapping("/cities")
    public Result<City> createCity(@Valid @RequestBody CityDTO dto, HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        return Result.success(adminService.createCity(dto, adminId, role));
    }

    @PutMapping("/cities/{id}")
    public Result<City> updateCity(@PathVariable Long id,
                                   @Valid @RequestBody CityDTO dto,
                                   HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        return Result.success(adminService.updateCity(id, dto, adminId, role));
    }

    @DeleteMapping("/cities/{id}")
    public Result<Void> deleteCity(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        adminService.deleteCity(id, adminId, role);
        return Result.success();
    }

    // ========== 景点管理 ==========

    @GetMapping("/attractions")
    public Result<PageResult<Attraction>> getAttractions(@RequestParam(required = false) Long cityId,
                                                         @Valid PageQuery query,
                                                         HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(PageResult.of(
                adminService.getAttractions(cityId, query.getPage(), query.getPageSize())));
    }

    @PostMapping("/attractions")
    public Result<Attraction> createAttraction(@Valid @RequestBody AttractionDTO dto,
                                               HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        return Result.success(adminService.createAttraction(dto, adminId, role));
    }

    @PutMapping("/attractions/{id}")
    public Result<Attraction> updateAttraction(@PathVariable Long id,
                                               @Valid @RequestBody AttractionDTO dto,
                                               HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        return Result.success(adminService.updateAttraction(id, dto, adminId, role));
    }

    @DeleteMapping("/attractions/{id}")
    public Result<Void> deleteAttraction(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        adminService.deleteAttraction(id, adminId, role);
        return Result.success();
    }

    /** 多图上传（返回 URL 列表） */
    @PostMapping("/images")
    public Result<List<String>> uploadImages(@RequestParam("files") List<MultipartFile> files,
                                             HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(adminService.uploadImages(files));
    }

    // ========== 帖子管理 ==========

    @GetMapping("/posts")
    public Result<PageResult<ForumPost>> getPosts(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @Valid PageQuery query,
                                                  HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(PageResult.of(
                adminService.getPosts(keyword, categoryId, query.getPage(), query.getPageSize())));
    }

    @PutMapping("/posts/{id}/status")
    public Result<ForumPost> updatePostStatus(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        Integer status = body.get("status") instanceof Number n ? n.intValue() : null;
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return Result.success(adminService.updatePostStatus(id, status, reason, adminId, role));
    }

    @PostMapping("/posts/batch-hide")
    public Result<Map<String, Object>> batchHidePosts(@Valid @RequestBody PostBatchDTO dto,
                                                      HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        int n = adminService.batchHidePosts(dto.getIds(), dto.getReason(), adminId, role);
        return Result.success(Map.of("count", n));
    }

    @PostMapping("/posts/batch-delete")
    public Result<Map<String, Object>> batchDeletePosts(@Valid @RequestBody PostBatchDTO dto,
                                                        HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        int n = adminService.batchDeletePosts(dto.getIds(), adminId, role);
        return Result.success(Map.of("count", n));
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        adminService.deletePost(id, adminId, role);
        return Result.success();
    }

    // ========== 行程推荐管理 ==========

    @GetMapping("/itineraries")
    public Result<PageResult<Itinerary>> getItineraries(@RequestParam(required = false) Integer recommended,
                                                        @RequestParam(required = false) String keyword,
                                                        @Valid PageQuery query,
                                                        HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(PageResult.of(
                adminService.getItineraries(recommended, keyword, query.getPage(), query.getPageSize())));
    }

    @PutMapping("/itineraries/{id}/recommend")
    public Result<Itinerary> recommendItinerary(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        requireAdmin(request);
        Long adminId = (Long) request.getAttribute("currentUserId");
        String role = (String) request.getAttribute("currentUserRole");
        Integer recommended = body.get("recommended") instanceof Number n ? n.intValue() : null;
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return Result.success(adminService.recommendItinerary(id, recommended, reason, adminId, role));
    }

    // ========== 操作日志 ==========

    @GetMapping("/logs")
    public Result<PageResult<OperationLog>> getLogs(@RequestParam(required = false) String targetType,
                                                    @Valid PageQuery query,
                                                    HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(PageResult.of(
                adminService.getLogs(targetType, query.getPage(), query.getPageSize())));
    }
}
