package com.travelmate.controller;

import com.travelmate.common.PageResult;
import com.travelmate.common.Result;
import com.travelmate.dto.CommentDTO;
import com.travelmate.dto.ForumPostCreateDTO;
import com.travelmate.dto.PageQuery;
import com.travelmate.entity.ForumCategory;
import com.travelmate.entity.ForumComment;
import com.travelmate.entity.ForumPost;
import com.travelmate.entity.Notification;
import com.travelmate.service.ForumService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 论坛控制器
 */
@RestController
@RequestMapping("/api/v1/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    // ========== 公开 ==========

    @GetMapping("/categories")
    public Result<List<ForumCategory>> getCategories() {
        return Result.success(forumService.getCategories());
    }

    @GetMapping("/posts")
    public Result<PageResult<ForumPost>> listPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @Valid PageQuery query,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(PageResult.of(
                forumService.listPosts(categoryId, keyword, query.getPage(), query.getPageSize(), userId)));
    }

    @GetMapping("/posts/{id}")
    public Result<ForumPost> getPostDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.getPostDetail(id, userId));
    }

    @GetMapping("/posts/{id}/edit")
    public Result<ForumPost> getPostForEdit(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.getPostForEdit(id, userId));
    }

    @GetMapping("/posts/{id}/comments")
    public Result<List<ForumComment>> getPostComments(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.getPostComments(id, userId));
    }

    // ========== 需登录 ==========

    @PostMapping("/posts")
    public Result<ForumPost> createPost(@Valid @RequestBody ForumPostCreateDTO dto,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.createPost(userId, dto));
    }

    @PutMapping("/posts/{id}")
    public Result<ForumPost> updatePost(@PathVariable Long id,
                                        @Valid @RequestBody ForumPostCreateDTO dto,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.updatePost(userId, id, dto));
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        forumService.deletePost(userId, id);
        return Result.success();
    }

    @PostMapping("/comments")
    public Result<ForumComment> addComment(@Valid @RequestBody CommentDTO dto,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.addComment(userId, dto));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        forumService.deleteComment(userId, id);
        return Result.success();
    }

    @PostMapping("/comments/{id}/like")
    public Result<Map<String, Object>> toggleCommentLike(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.toggleCommentLike(userId, id));
    }

    @PostMapping("/comments/{id}/not-interested")
    public Result<Map<String, Object>> toggleCommentNotInterested(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.toggleCommentNotInterested(userId, id));
    }

    @PostMapping("/likes")
    public Result<Map<String, Object>> toggleLike(@RequestBody Map<String, Long> body,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        Long postId = body.get("postId");
        boolean liked = forumService.toggleLike(userId, postId);
        return Result.success(Map.of("liked", liked));
    }

    @GetMapping("/notifications")
    public Result<List<Notification>> getMyNotifications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(forumService.getMyNotifications(userId));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Void> markNotificationRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        forumService.markNotificationRead(userId, id);
        return Result.success();
    }

    @PutMapping("/notifications/read-all")
    public Result<Void> markAllNotificationsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        forumService.markAllNotificationsRead(userId);
        return Result.success();
    }
}
