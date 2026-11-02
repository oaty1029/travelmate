package com.travelmate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.common.BusinessException;
import com.travelmate.common.PageUtil;
import com.travelmate.dto.CommentDTO;
import com.travelmate.dto.ForumPostCreateDTO;
import com.travelmate.entity.ForumCategory;
import com.travelmate.entity.ForumComment;
import com.travelmate.entity.ForumCommentReaction;
import com.travelmate.entity.ForumLike;
import com.travelmate.entity.ForumPost;
import com.travelmate.entity.ForumPostView;
import com.travelmate.entity.Itinerary;
import com.travelmate.entity.Notification;
import com.travelmate.mapper.ForumCategoryMapper;
import com.travelmate.mapper.ForumCommentMapper;
import com.travelmate.mapper.ForumCommentReactionMapper;
import com.travelmate.mapper.ForumLikeMapper;
import com.travelmate.mapper.ForumPostMapper;
import com.travelmate.mapper.ForumPostViewMapper;
import com.travelmate.mapper.ItineraryMapper;
import com.travelmate.mapper.NotificationMapper;
import com.travelmate.mapper.UserMapper;
import com.travelmate.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 论坛服务
 */
@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumPostMapper postMapper;
    private final ForumCommentMapper commentMapper;
    private final ForumLikeMapper likeMapper;
    private final ForumCategoryMapper categoryMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final ItineraryMapper itineraryMapper;
    private final ForumPostViewMapper postViewMapper;
    private final ForumCommentReactionMapper commentReactionMapper;
    private final ObjectMapper objectMapper;

    public List<ForumCategory> getCategories() {
        return categoryMapper.selectList(new QueryWrapper<ForumCategory>().orderByAsc("sort_order"));
    }

    /** 分页查帖子（已发布）；查看人数仅帖主可见 */
    public IPage<ForumPost> listPosts(Long categoryId, String keyword, int page, int size, Long currentUserId) {
        Page<ForumPost> pageParam = PageUtil.of(page, size);
        QueryWrapper<ForumPost> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        if (categoryId != null) qw.eq("category_id", categoryId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            if (kw.length() > 50) throw BusinessException.badRequest("搜索关键词最长 50 字");
            qw.and(w -> w.like("title", kw).or().like("content", kw));
        }
        qw.orderByDesc("created_at");
        IPage<ForumPost> result = postMapper.selectPage(pageParam, qw);
        fillPostDisplay(result.getRecords());
        maskViewCount(result.getRecords(), currentUserId);
        return result;
    }

    /** 帖子详情：去重浏览 + 填充是否已点赞 + 查看人数仅帖主可见 */
    @Transactional
    public ForumPost getPostDetail(Long postId, Long currentUserId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在或已被下架");
        }

        // 去重浏览：登录用户首次浏览才 +1（同一用户重复点击不累计）
        boolean firstView = false;
        if (currentUserId != null) {
            ForumPostView pv = new ForumPostView();
            pv.setPostId(postId);
            pv.setUserId(currentUserId);
            try {
                postViewMapper.insert(pv);
                firstView = true;
            } catch (DuplicateKeyException e) {
                // 已浏览过，不重复计数
            }
        }
        if (firstView) {
            post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
            postMapper.updateById(post);
        }

        fillPostDisplay(List.of(post));
        if (currentUserId != null) {
            post.setLiked(likeMapper.selectCount(
                    new QueryWrapper<ForumLike>().eq("user_id", currentUserId).eq("post_id", postId)) > 0);
        }
        // 查看人数仅发帖帖主可见
        if (!post.getUserId().equals(currentUserId)) {
            post.setViewCount(null);
        }
        attachItinerary(post);
        return post;
    }

    /** 编辑用帖子加载：不增加浏览量，并校验归属 */
    public ForumPost getPostForEdit(Long postId, Long currentUserId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在或已被下架");
        }
        if (currentUserId == null || !post.getUserId().equals(currentUserId)) {
            throw BusinessException.forbidden("无权编辑此帖子");
        }
        fillPostDisplay(List.of(post));
        return post;
    }

    /** 创建帖子 */
    public ForumPost createPost(Long userId, ForumPostCreateDTO dto) {
        ForumCategory cat = categoryMapper.selectById(dto.getCategoryId());
        if (cat == null) throw BusinessException.notFound("分类不存在");

        validateItineraryOwner(userId, dto.getItineraryId());

        ForumPost post = new ForumPost();
        post.setCategoryId(dto.getCategoryId());
        post.setUserId(userId);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCoverUrl(dto.getCoverUrl());
        post.setItineraryId(dto.getItineraryId());
        post.setStatus(1);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        postMapper.insert(post);
        return post;
    }

    /** 更新帖子 */
    public ForumPost updatePost(Long userId, Long postId, ForumPostCreateDTO dto) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null) throw BusinessException.notFound("帖子不存在");
        if (!post.getUserId().equals(userId)) throw BusinessException.forbidden("无权操作此帖子");

        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getContent() != null) post.setContent(dto.getContent());
        if (dto.getCategoryId() != null) post.setCategoryId(dto.getCategoryId());
        if (dto.getCoverUrl() != null) post.setCoverUrl(dto.getCoverUrl());
        if (dto.getItineraryId() != null) {
            validateItineraryOwner(userId, dto.getItineraryId());
            post.setItineraryId(dto.getItineraryId());
        }
        postMapper.updateById(post);
        return post;
    }

    /** 删除帖子 + 级联 */
    @Transactional
    public void deletePost(Long userId, Long postId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null) throw BusinessException.notFound("帖子不存在");
        if (!post.getUserId().equals(userId)) throw BusinessException.forbidden("无权操作此帖子");

        List<Long> commentIds = commentMapper.selectList(
                        new QueryWrapper<ForumComment>().eq("post_id", postId).select("id"))
                .stream().map(ForumComment::getId).collect(Collectors.toList());
        likeMapper.delete(new QueryWrapper<ForumLike>().eq("post_id", postId));
        commentMapper.delete(new QueryWrapper<ForumComment>().eq("post_id", postId));
        if (!commentIds.isEmpty()) {
            commentReactionMapper.delete(new QueryWrapper<ForumCommentReaction>().in("comment_id", commentIds));
        }
        postViewMapper.delete(new QueryWrapper<ForumPostView>().eq("post_id", postId));
        notificationMapper.delete(new QueryWrapper<Notification>().eq("post_id", postId));
        postMapper.deleteById(postId);
    }

    /** 帖子评论列表：顶层评论 + 嵌套回复（帖子不存在 404） */
    public List<ForumComment> getPostComments(Long postId, Long currentUserId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在或已被下架");
        }
        List<ForumComment> all = commentMapper.selectList(
                new QueryWrapper<ForumComment>().eq("post_id", postId).eq("status", 1).orderByAsc("created_at")
        );
        fillCommentDisplay(all);
        fillCommentReactions(all, currentUserId);
        return buildCommentTree(all);
    }

    /** 删除评论（评论作者或帖子作者可删）；顶层评论级联删除其回复 */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        ForumComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw BusinessException.notFound("评论不存在");
        ForumPost post = postMapper.selectById(comment.getPostId());
        boolean isCommentAuthor = comment.getUserId().equals(userId);
        boolean isPostAuthor = post != null && post.getUserId().equals(userId);
        if (!isCommentAuthor && !isPostAuthor) throw BusinessException.forbidden("无权删除此评论");

        // 收集要删除的评论 id（顶层评论级联删除其下回复）
        List<Long> deleteIds = new ArrayList<>();
        deleteIds.add(commentId);
        if (comment.getParentId() == null) {
            commentMapper.selectList(new QueryWrapper<ForumComment>().eq("parent_id", commentId))
                    .forEach(r -> deleteIds.add(r.getId()));
        }
        commentMapper.deleteBatchIds(deleteIds);
        commentReactionMapper.delete(new QueryWrapper<ForumCommentReaction>().in("comment_id", deleteIds));

        if (post != null) {
            post.setCommentCount(Math.max(0, (post.getCommentCount() == null ? 0 : post.getCommentCount()) - deleteIds.size()));
            postMapper.updateById(post);
        }
    }

    /** 加评论/回复 + 更新 post.commentCount + 通知（顶层评论通知楼主，回复通知被回复用户） */
    @Transactional
    public ForumComment addComment(Long userId, CommentDTO dto) {
        ForumPost post = postMapper.selectById(dto.getPostId());
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在");
        }
        ForumComment comment = new ForumComment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setLikeCount(0);
        comment.setNotInterestedCount(0);
        comment.setStatus(1);

        // 回复：统一挂到顶层评论下，记录被回复的具体用户
        Long replyToUserId = null;
        if (dto.getParentId() != null) {
            ForumComment parent = commentMapper.selectById(dto.getParentId());
            if (parent == null || !parent.getPostId().equals(dto.getPostId()) || parent.getStatus() != 1) {
                throw BusinessException.badRequest("被回复的评论不存在");
            }
            Long topId = parent.getParentId() == null ? parent.getId() : parent.getParentId();
            comment.setParentId(topId);
            comment.setReplyToUserId(parent.getUserId());
            replyToUserId = parent.getUserId();
        }
        commentMapper.insert(comment);

        post.setCommentCount(post.getCommentCount() == null ? 1 : post.getCommentCount() + 1);
        postMapper.updateById(post);

        // 通知：回复 → 被回复用户；顶层评论 → 楼主
        Long targetUserId = replyToUserId != null ? replyToUserId : post.getUserId();
        if (!targetUserId.equals(userId)) {
            Notification n = new Notification();
            n.setUserId(targetUserId);
            n.setType(replyToUserId != null ? "reply" : "comment");
            n.setTitle(replyToUserId != null ? "新回复提醒" : "新评论提醒");
            n.setContent(replyToUserId != null
                    ? "有用户回复了你的评论：" + truncate(comment.getContent(), 80)
                    : "有用户评论了你的帖子：" + truncate(post.getTitle(), 80));
            n.setPostId(post.getId());
            n.setFromUserId(userId);
            n.setIsRead(0);
            notificationMapper.insert(n);
        }
        return comment;
    }

    /** 点赞/取消点赞 */
    @Transactional
    public boolean toggleLike(Long userId, Long postId) {
        if (postId == null) throw BusinessException.badRequest("postId 必填");
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在");
        }
        ForumLike existing = likeMapper.selectOne(
                new QueryWrapper<ForumLike>().eq("user_id", userId).eq("post_id", postId)
        );
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() == null ? 0 : post.getLikeCount() - 1));
            postMapper.updateById(post);
            return false;
        } else {
            ForumLike like = new ForumLike();
            like.setUserId(userId);
            like.setPostId(postId);
            likeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
            postMapper.updateById(post);

            // 通知楼主
            if (!post.getUserId().equals(userId)) {
                Notification n = new Notification();
                n.setUserId(post.getUserId());
                n.setType("like");
                n.setTitle("新点赞提醒");
                n.setContent("有用户点赞了你的帖子：" + truncate(post.getTitle(), 80));
                n.setPostId(post.getId());
                n.setFromUserId(userId);
                n.setIsRead(0);
                notificationMapper.insert(n);
            }
            return true;
        }
    }

    /** 评论点赞/取消点赞（与不感兴趣互斥） */
    @Transactional
    public Map<String, Object> toggleCommentLike(Long userId, Long commentId) {
        return toggleCommentReaction(userId, commentId, "like");
    }

    /** 评论标记不感兴趣/取消（与点赞互斥） */
    @Transactional
    public Map<String, Object> toggleCommentNotInterested(Long userId, Long commentId) {
        return toggleCommentReaction(userId, commentId, "not_interested");
    }

    private Map<String, Object> toggleCommentReaction(Long userId, Long commentId, String type) {
        if (commentId == null) throw BusinessException.badRequest("commentId 必填");
        ForumComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw BusinessException.notFound("评论不存在");
        }
        String opposite = "like".equals(type) ? "not_interested" : "like";

        ForumCommentReaction existing = commentReactionMapper.selectOne(
                new QueryWrapper<ForumCommentReaction>()
                        .eq("user_id", userId).eq("comment_id", commentId).eq("type", type));
        if (existing != null) {
            // 取消当前反应
            commentReactionMapper.deleteById(existing.getId());
            decrementReactionCount(comment, type);
        } else {
            // 添加当前反应，并移除互斥反应
            ForumCommentReaction r = new ForumCommentReaction();
            r.setUserId(userId);
            r.setCommentId(commentId);
            r.setType(type);
            commentReactionMapper.insert(r);
            incrementReactionCount(comment, type);

            // 点赞评论：通知评论作者（排除自己赞自己）
            if ("like".equals(type) && !comment.getUserId().equals(userId)) {
                Notification n = new Notification();
                n.setUserId(comment.getUserId());
                n.setType("comment_like");
                n.setTitle("评论获赞提醒");
                n.setContent("有用户点赞了你的评论：" + truncate(comment.getContent(), 80));
                n.setPostId(comment.getPostId());
                n.setFromUserId(userId);
                n.setIsRead(0);
                notificationMapper.insert(n);
            }

            ForumCommentReaction opp = commentReactionMapper.selectOne(
                    new QueryWrapper<ForumCommentReaction>()
                            .eq("user_id", userId).eq("comment_id", commentId).eq("type", opposite));
            if (opp != null) {
                commentReactionMapper.deleteById(opp.getId());
                decrementReactionCount(comment, opposite);
            }
        }
        commentMapper.updateById(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", "like".equals(type) ? (existing == null) : false);
        result.put("notInterested", "not_interested".equals(type) ? (existing == null) : false);
        result.put("likeCount", comment.getLikeCount());
        result.put("notInterestedCount", comment.getNotInterestedCount());
        return result;
    }

    private void incrementReactionCount(ForumComment c, String type) {
        if ("like".equals(type)) {
            c.setLikeCount(c.getLikeCount() == null ? 1 : c.getLikeCount() + 1);
        } else {
            c.setNotInterestedCount(c.getNotInterestedCount() == null ? 1 : c.getNotInterestedCount() + 1);
        }
    }

    private void decrementReactionCount(ForumComment c, String type) {
        if ("like".equals(type)) {
            c.setLikeCount(Math.max(0, c.getLikeCount() == null ? 0 : c.getLikeCount() - 1));
        } else {
            c.setNotInterestedCount(Math.max(0, c.getNotInterestedCount() == null ? 0 : c.getNotInterestedCount() - 1));
        }
    }

    /** 我的未读通知 */
    public List<Notification> getMyNotifications(Long userId) {
        return notificationMapper.selectList(
                new QueryWrapper<Notification>()
                        .eq("user_id", userId)
                        .eq("is_read", 0)
                        .orderByDesc("created_at")
                        .last("LIMIT 50")
        );
    }

    /** 标记单条通知已读 */
    public void markNotificationRead(Long userId, Long notificationId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null) throw BusinessException.notFound("通知不存在");
        if (!n.getUserId().equals(userId)) throw BusinessException.forbidden("无权操作此通知");
        if (n.getIsRead() == null || n.getIsRead() == 0) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }

    /** 全部通知标记已读 */
    public void markAllNotificationsRead(Long userId) {
        notificationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Notification>()
                .eq("user_id", userId).eq("is_read", 0).set("is_read", 1));
    }

    /** 通知 content 列 varchar(191)，标题需截断保证不超长 */
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** 校验挂载行程存在且属于当前用户 */
    private void validateItineraryOwner(Long userId, Long itineraryId) {
        if (itineraryId == null) return;
        Itinerary it = itineraryMapper.selectById(itineraryId);
        if (it == null) throw BusinessException.notFound("挂载的行程不存在");
        if (!it.getUserId().equals(userId)) throw BusinessException.forbidden("只能挂载自己的行程");
    }

    /** 详情接口填充挂载行程内容（result_json） */
    private void attachItinerary(ForumPost post) {
        if (post.getItineraryId() == null) return;
        Itinerary it = itineraryMapper.selectById(post.getItineraryId());
        if (it == null || it.getResultJson() == null) return;
        try {
            post.setItinerary(objectMapper.readValue(it.getResultJson(),
                    new TypeReference<Map<String, Object>>() {}));
        } catch (Exception ignored) {
            // 行程 JSON 解析失败则不展示
        }
    }

    /** 批量填充帖子展示字段（作者昵称/头像、分类名），供论坛与管理后台共用 */
    public void fillPostDisplay(List<ForumPost> posts) {
        if (posts == null || posts.isEmpty()) return;
        Set<Long> userIds = posts.stream().map(ForumPost::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> catIds = posts.stream().map(ForumPost::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, ForumCategory> cats = catIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(catIds).stream()
                        .collect(Collectors.toMap(ForumCategory::getId, Function.identity()));
        for (ForumPost p : posts) {
            User u = users.get(p.getUserId());
            if (u != null) {
                p.setAuthorName(u.getNickname());
                p.setAuthorAvatar(u.getAvatarUrl());
            }
            ForumCategory c = cats.get(p.getCategoryId());
            if (c != null) p.setCategoryName(c.getName());
        }
    }

    /** 批量填充评论展示字段（作者昵称/头像、被回复人昵称） */
    private void fillCommentDisplay(List<ForumComment> comments) {
        if (comments == null || comments.isEmpty()) return;
        Set<Long> userIds = comments.stream().map(ForumComment::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        comments.stream().map(ForumComment::getReplyToUserId)
                .filter(Objects::nonNull).forEach(userIds::add);
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        for (ForumComment c : comments) {
            User u = users.get(c.getUserId());
            if (u != null) {
                c.setAuthorName(u.getNickname());
                c.setAuthorAvatar(u.getAvatarUrl());
            }
            if (c.getReplyToUserId() != null) {
                User ru = users.get(c.getReplyToUserId());
                if (ru != null) c.setReplyToUserName(ru.getNickname());
            }
        }
    }

    /** 填充评论是否已点赞/不感兴趣（需当前登录用户） */
    private void fillCommentReactions(List<ForumComment> comments, Long currentUserId) {
        if (currentUserId == null || comments == null || comments.isEmpty()) return;
        Set<Long> ids = comments.stream().map(ForumComment::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return;
        List<ForumCommentReaction> reactions = commentReactionMapper.selectList(
                new QueryWrapper<ForumCommentReaction>().eq("user_id", currentUserId).in("comment_id", ids));
        Set<Long> likedIds = reactions.stream()
                .filter(r -> "like".equals(r.getType()))
                .map(ForumCommentReaction::getCommentId).collect(Collectors.toSet());
        Set<Long> notInterestedIds = reactions.stream()
                .filter(r -> "not_interested".equals(r.getType()))
                .map(ForumCommentReaction::getCommentId).collect(Collectors.toSet());
        for (ForumComment c : comments) {
            c.setLiked(likedIds.contains(c.getId()));
            c.setNotInterested(notInterestedIds.contains(c.getId()));
        }
    }

    /** 把平铺评论组装成顶层评论 + 嵌套回复 */
    private List<ForumComment> buildCommentTree(List<ForumComment> all) {
        List<ForumComment> top = new ArrayList<>();
        Map<Long, List<ForumComment>> byParent = new HashMap<>();
        for (ForumComment c : all) {
            if (c.getParentId() == null) {
                top.add(c);
            } else {
                byParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }
        for (ForumComment t : top) {
            List<ForumComment> replies = byParent.getOrDefault(t.getId(), List.of());
            t.setReplies(replies);
            t.setReplyCount(replies.size());
        }
        return top;
    }

    /** 查看人数仅帖主可见：非帖主一律置空 */
    private void maskViewCount(List<ForumPost> posts, Long currentUserId) {
        if (posts == null) return;
        for (ForumPost p : posts) {
            if (currentUserId == null || !p.getUserId().equals(currentUserId)) {
                p.setViewCount(null);
            }
        }
    }
}
