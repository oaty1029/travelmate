package com.travelmate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.common.BusinessException;
import com.travelmate.common.PageUtil;
import com.travelmate.dto.AttractionDTO;
import com.travelmate.dto.CityDTO;
import com.travelmate.entity.*;
import com.travelmate.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员服务 - 仅 AdminController 中使用（Controller 里手动校验 role）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final CityMapper cityMapper;
    private final ProvinceMapper provinceMapper;
    private final ForumPostMapper postMapper;
    private final ForumCommentMapper commentMapper;
    private final ForumLikeMapper likeMapper;
    private final NotificationMapper notificationMapper;
    private final OperationLogMapper logMapper;
    private final AiConfigMapper aiConfigMapper;
    private final ItineraryMapper itineraryMapper;
    private final AttractionMapper attractionMapper;
    private final ForumService forumService;
    private final ObjectMapper objectMapper;
    private final ImageStorageService imageStorageService;

    private static final Set<String> ATTRACTION_CATEGORIES = Set.of(
            "自然景观", "历史遗迹", "博物馆", "宗教", "主题公园", "街区");

    // ============ 用户管理 ============

    public IPage<User> getUsers(String nickname, int page, int size) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        if (nickname != null && !nickname.isBlank()) {
            String kw = nickname.trim();
            if (kw.length() > 50) throw BusinessException.badRequest("搜索昵称最长 50 字");
            qw.like("nickname", kw);
        }
        qw.orderByDesc("created_at");
        return userMapper.selectPage(PageUtil.of(page, size), qw);
    }

    public User getUserById(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw BusinessException.notFound("用户不存在");
        return u;
    }

    public User updateUserStatus(Long id, Integer status, Long adminId, String role) {
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("用户状态不合法（0=禁用 1=启用）");
        }
        if (id.equals(adminId)) {
            throw BusinessException.badRequest("不能操作自己的账号");
        }
        User u = userMapper.selectById(id);
        if (u == null) throw BusinessException.notFound("用户不存在");
        if (!"user".equals(u.getRole())) {
            throw BusinessException.badRequest("不能操作管理员账号");
        }
        u.setStatus(status);
        userMapper.updateById(u);
        writeLog(adminId, role, "status", "user", id, status == 0 ? "禁用" : "启用");
        return u;
    }

    @Transactional
    public void deleteUser(Long id, Long adminId, String role) {
        User u = userMapper.selectById(id);
        if (u == null) throw BusinessException.notFound("用户不存在");
        if (u.getId().equals(adminId)) {
            throw BusinessException.badRequest("不能删除自己");
        }
        if (!"user".equals(u.getRole())) {
            throw BusinessException.badRequest("不能删除管理员账号");
        }

        List<Long> postIds = postMapper.selectList(
                new QueryWrapper<ForumPost>().eq("user_id", id).select("id"))
                .stream().map(ForumPost::getId).toList();
        if (!postIds.isEmpty()) {
            commentMapper.delete(new QueryWrapper<ForumComment>().in("post_id", postIds));
            likeMapper.delete(new QueryWrapper<ForumLike>().in("post_id", postIds));
        }
        postMapper.delete(new QueryWrapper<ForumPost>().eq("user_id", id));
        commentMapper.delete(new QueryWrapper<ForumComment>().eq("user_id", id));
        likeMapper.delete(new QueryWrapper<ForumLike>().eq("user_id", id));
        notificationMapper.delete(new QueryWrapper<Notification>()
                .eq("user_id", id).or().eq("from_user_id", id));

        userMapper.deleteById(id);
        writeLog(adminId, role, "delete", "user", id, null);
        log.info("Admin 删除用户 id={}，级联清理 posts={}", id, postIds.size());
    }

    // ============ 城市管理 ============

    public IPage<City> getCities(String keyword, Long provinceId, int page, int size) {
        QueryWrapper<City> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            if (kw.length() > 50) throw BusinessException.badRequest("搜索关键词最长 50 字");
            qw.like("name", kw);
        }
        if (provinceId != null) qw.eq("province_id", provinceId);
        // 热门城市优先，再按省份、id 排序
        qw.orderByDesc("is_popular").orderByAsc("province_id").orderByAsc("id");
        IPage<City> result = cityMapper.selectPage(PageUtil.of(page, size), qw);
        List<City> records = result.getRecords();
        if (!records.isEmpty()) {
            Set<Long> pids = new HashSet<>();
            for (City c : records) if (c.getProvinceId() != null) pids.add(c.getProvinceId());
            Map<Long, Province> pm = new HashMap<>();
            if (!pids.isEmpty()) {
                for (Province p : provinceMapper.selectBatchIds(pids)) pm.put(p.getId(), p);
            }
            for (City c : records) {
                Province p = pm.get(c.getProvinceId());
                if (p != null) c.setProvinceName(p.getName());
            }
        }
        return result;
    }

    public City createCity(CityDTO dto, Long adminId, String role) {
        validateCity(dto);
        City city = new City();
        applyCity(city, dto);
        cityMapper.insert(city);
        writeLog(adminId, role, "create", "city", city.getId(), city.getName());
        return city;
    }

    public City updateCity(Long id, CityDTO dto, Long adminId, String role) {
        City existing = cityMapper.selectById(id);
        if (existing == null) throw BusinessException.notFound("城市不存在");
        validateCity(dto);
        UpdateWrapper<City> uw = new UpdateWrapper<>();
        uw.eq("id", id)
                .set("name", dto.getName())
                .set("pinyin", dto.getPinyin())
                .set("province_id", dto.getProvinceId())
                .set("description", dto.getDescription())
                .set("specialty_food", dto.getSpecialtyFood())
                .set("local_culture", dto.getLocalCulture())
                .set("image_url", dto.getImageUrl())
                .set("best_season", dto.getBestSeason())
                .set("tags", dto.getTags())
                .set("popularity", dto.getPopularity() == null ? existing.getPopularity() : dto.getPopularity())
                .set("is_popular", dto.getIsPopular() == null ? existing.getIsPopular() : dto.getIsPopular())
                .set("lat", dto.getLat())
                .set("lng", dto.getLng())
                .set("status", dto.getStatus() == null ? existing.getStatus() : dto.getStatus());
        cityMapper.update(null, uw);
        writeLog(adminId, role, "update", "city", id, dto.getName());
        return cityMapper.selectById(id);
    }

    @Transactional
    public void deleteCity(Long id, Long adminId, String role) {
        City existing = cityMapper.selectById(id);
        if (existing == null) throw BusinessException.notFound("城市不存在");

        cityMapper.deleteById(id);
        writeLog(adminId, role, "delete", "city", id, existing.getName());
        log.info("Admin 删除城市 id={}", id);
    }

    public Map<String, String> uploadImage(MultipartFile file) {
        return Map.of("url", imageStorageService.storeImage(file));
    }

    public void clearCityImage(Long id, Long adminId, String role) {
        City city = cityMapper.selectById(id);
        if (city == null) throw BusinessException.notFound("城市不存在");
        imageStorageService.deleteImage(city.getImageUrl());
        UpdateWrapper<City> uw = new UpdateWrapper<>();
        uw.eq("id", id).set("image_url", null);
        cityMapper.update(null, uw);
        writeLog(adminId, role, "delete_image", "city", id, city.getName());
    }

    private void validateCity(CityDTO dto) {
        Province p = provinceMapper.selectById(dto.getProvinceId());
        if (p == null) throw BusinessException.notFound("省份不存在");
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw BusinessException.badRequest("城市状态不合法（0=下架 1=上架）");
        }
        validateJsonField(dto.getTags(), "标签");
    }

    private void applyCity(City city, CityDTO dto) {
        city.setName(dto.getName());
        city.setPinyin(dto.getPinyin());
        city.setProvinceId(dto.getProvinceId());
        city.setDescription(dto.getDescription());
        city.setSpecialtyFood(dto.getSpecialtyFood());
        city.setLocalCulture(dto.getLocalCulture());
        city.setImageUrl(dto.getImageUrl());
        city.setBestSeason(dto.getBestSeason());
        city.setTags(dto.getTags());
        city.setPopularity(dto.getPopularity() == null ? 0 : dto.getPopularity());
        city.setIsPopular(dto.getIsPopular() == null ? 0 : dto.getIsPopular());
        city.setLat(dto.getLat());
        city.setLng(dto.getLng());
        city.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private void validateJsonField(String value, String label) {
        if (value == null || value.isBlank()) return;
        try {
            objectMapper.readTree(value);
        } catch (Exception e) {
            throw BusinessException.badRequest(label + "必须是合法 JSON");
        }
    }

    // ============ 景点管理 ============

    public IPage<Attraction> getAttractions(Long cityId, int page, int size) {
        QueryWrapper<Attraction> qw = new QueryWrapper<>();
        if (cityId != null) qw.eq("city_id", cityId);
        qw.orderByAsc("city_id").orderByAsc("id");
        return attractionMapper.selectPage(PageUtil.of(page, size), qw);
    }

    public Attraction createAttraction(AttractionDTO dto, Long adminId, String role) {
        validateAttraction(dto);
        Attraction a = new Attraction();
        applyAttraction(a, dto);
        attractionMapper.insert(a);
        writeLog(adminId, role, "create", "attraction", a.getId(), a.getName());
        return a;
    }

    public Attraction updateAttraction(Long id, AttractionDTO dto, Long adminId, String role) {
        Attraction existing = attractionMapper.selectById(id);
        if (existing == null) throw BusinessException.notFound("景点不存在");
        validateAttraction(dto);
        UpdateWrapper<Attraction> uw = new UpdateWrapper<>();
        uw.eq("id", id)
                .set("city_id", dto.getCityId())
                .set("name", dto.getName())
                .set("category", dto.getCategory())
                .set("description", dto.getDescription())
                .set("rating", dto.getRating())
                .set("ticket_price", dto.getTicketPrice())
                .set("suggest_duration", dto.getSuggestDuration())
                .set("open_time", dto.getOpenTime())
                .set("best_season", dto.getBestSeason())
                .set("address", dto.getAddress())
                .set("image_urls", dto.getImageUrls())
                .set("status", dto.getStatus() == null ? existing.getStatus() : dto.getStatus());
        attractionMapper.update(null, uw);
        writeLog(adminId, role, "update", "attraction", id, dto.getName());
        return attractionMapper.selectById(id);
    }

    public void deleteAttraction(Long id, Long adminId, String role) {
        Attraction existing = attractionMapper.selectById(id);
        if (existing == null) throw BusinessException.notFound("景点不存在");
        attractionMapper.deleteById(id);
        writeLog(adminId, role, "delete", "attraction", id, existing.getName());
    }

    /** 多图上传：逐个复用单图校验/存储逻辑 */
    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw BusinessException.badRequest("请选择图片文件");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : files) {
            urls.add(uploadImage(f).get("url"));
        }
        return urls;
    }

    private void validateAttraction(AttractionDTO dto) {
        if (cityMapper.selectById(dto.getCityId()) == null) {
            throw BusinessException.notFound("所属城市不存在");
        }
        if (!ATTRACTION_CATEGORIES.contains(dto.getCategory())) {
            throw BusinessException.badRequest("景点分类不合法");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw BusinessException.badRequest("景点状态不合法（0=下架 1=上架）");
        }
        validateJsonField(dto.getImageUrls(), "图片列表");
    }

    private void applyAttraction(Attraction a, AttractionDTO dto) {
        a.setCityId(dto.getCityId());
        a.setName(dto.getName());
        a.setCategory(dto.getCategory());
        a.setDescription(dto.getDescription());
        a.setRating(dto.getRating());
        a.setTicketPrice(dto.getTicketPrice());
        a.setSuggestDuration(dto.getSuggestDuration());
        a.setOpenTime(dto.getOpenTime());
        a.setBestSeason(dto.getBestSeason());
        a.setAddress(dto.getAddress());
        a.setImageUrls(dto.getImageUrls());
        a.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    // ============ 帖子管理 ============

    public IPage<ForumPost> getPosts(String keyword, Long categoryId, int page, int size) {
        QueryWrapper<ForumPost> qw = new QueryWrapper<>();
        if (categoryId != null) qw.eq("category_id", categoryId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            if (kw.length() > 50) throw BusinessException.badRequest("搜索关键词最长 50 字");
            List<Long> authorIds = userMapper.selectList(
                    new QueryWrapper<User>().like("nickname", kw).select("id"))
                    .stream().map(User::getId).toList();
            qw.and(w -> {
                w.like("title", kw);
                if (!authorIds.isEmpty()) w.or().in("user_id", authorIds);
            });
        }
        qw.orderByDesc("created_at");
        IPage<ForumPost> result = postMapper.selectPage(PageUtil.of(page, size), qw);
        forumService.fillPostDisplay(result.getRecords());
        return result;
    }

    public ForumPost updatePostStatus(Long id, Integer status, String reason,
                                      Long adminId, String role) {
        ForumPost p = postMapper.selectById(id);
        if (p == null) throw BusinessException.notFound("帖子不存在");
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw BusinessException.badRequest("帖子状态不合法（0=草稿 1=已发布 2=已隐藏）");
        }
        UpdateWrapper<ForumPost> uw = new UpdateWrapper<>();
        uw.eq("id", id).set("status", status);
        if (status == 2) {
            uw.set("hidden_at", LocalDateTime.now())
                    .set("hidden_by", adminId)
                    .set("hidden_by_role", role)
                    .set("hide_reason", reason);
        } else if (status == 1) {
            uw.set("hidden_at", null).set("hidden_by", null)
                    .set("hidden_by_role", null).set("hide_reason", null);
        }
        postMapper.update(null, uw);
        writeLog(adminId, role, "status", "post", id,
                "status=" + status + (reason == null || reason.isBlank() ? "" : " 原因:" + reason));
        p.setStatus(status);
        return p;
    }

    /** 批量隐藏帖子（全部标记 status=2 + 隐藏信息） */
    @Transactional
    public int batchHidePosts(List<Long> ids, String reason, Long adminId, String role) {
        UpdateWrapper<ForumPost> uw = new UpdateWrapper<>();
        uw.in("id", ids)
                .set("status", 2)
                .set("hidden_at", LocalDateTime.now())
                .set("hidden_by", adminId)
                .set("hidden_by_role", role)
                .set("hide_reason", reason);
        int n = postMapper.update(null, uw);
        writeLog(adminId, role, "batch_hide", "post", ids.get(0),
                "批量隐藏 " + n + " 条" + (reason == null || reason.isBlank() ? "" : " 原因:" + reason));
        return n;
    }

    /** 批量删除帖子（含评论/点赞级联） */
    @Transactional
    public int batchDeletePosts(List<Long> ids, Long adminId, String role) {
        if (!ids.isEmpty()) {
            commentMapper.delete(new QueryWrapper<ForumComment>().in("post_id", ids));
            likeMapper.delete(new QueryWrapper<ForumLike>().in("post_id", ids));
            notificationMapper.delete(new QueryWrapper<Notification>().in("post_id", ids));
        }
        int n = postMapper.delete(new QueryWrapper<ForumPost>().in("id", ids));
        writeLog(adminId, role, "batch_delete", "post", ids.get(0), "批量删除 " + n + " 条");
        return n;
    }

    @Transactional
    public void deletePost(Long id, Long adminId, String role) {
        ForumPost p = postMapper.selectById(id);
        if (p == null) throw BusinessException.notFound("帖子不存在");
        commentMapper.delete(new QueryWrapper<ForumComment>().eq("post_id", id));
        likeMapper.delete(new QueryWrapper<ForumLike>().eq("post_id", id));
        notificationMapper.delete(new QueryWrapper<Notification>().eq("post_id", id));
        postMapper.deleteById(id);
        writeLog(adminId, role, "delete", "post", id, p.getTitle());
    }

    // ============ AI 配置 ============

    public AiConfig getAiConfig() {
        return aiConfigMapper.selectOne(
                new QueryWrapper<AiConfig>().orderByAsc("id").last("LIMIT 1"));
    }

    public AiConfig updateAiConfig(AiConfig dto, Long adminId, String role) {
        if (dto.getProvider() == null || dto.getProvider().isBlank()) {
            throw BusinessException.badRequest("平台不能为空");
        }
        if (dto.getBaseUrl() == null || dto.getBaseUrl().isBlank()) {
            throw BusinessException.badRequest("接口地址不能为空");
        }
        if (dto.getModel() == null || dto.getModel().isBlank()) {
            throw BusinessException.badRequest("模型不能为空");
        }
        if (dto.getTemperature() != null
                && (dto.getTemperature().compareTo(BigDecimal.ZERO) < 0
                    || dto.getTemperature().compareTo(BigDecimal.valueOf(2)) > 0)) {
            throw BusinessException.badRequest("温度参数范围 0~2");
        }
        if (dto.getMaxTokens() != null && dto.getMaxTokens() <= 0) {
            throw BusinessException.badRequest("max_tokens 必须大于 0");
        }
        if (dto.getEnabled() != null && dto.getEnabled() != 0 && dto.getEnabled() != 1) {
            throw BusinessException.badRequest("启用状态不合法（0=停用 1=启用）");
        }

        AiConfig existing = getAiConfig();
        AiConfig cfg = existing != null ? existing : new AiConfig();
        cfg.setProvider(dto.getProvider().trim());
        cfg.setBaseUrl(dto.getBaseUrl().trim());
        cfg.setApiKey(dto.getApiKey() == null ? "" : dto.getApiKey().trim());
        cfg.setModel(dto.getModel().trim());
        cfg.setTemperature(dto.getTemperature() == null ? BigDecimal.valueOf(1.0) : dto.getTemperature());
        cfg.setMaxTokens(dto.getMaxTokens() == null ? 4096 : dto.getMaxTokens());
        cfg.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        if (existing != null) {
            aiConfigMapper.updateById(cfg);
        } else {
            aiConfigMapper.insert(cfg);
        }
        writeLog(adminId, role, "update", "ai_config", cfg.getId(), cfg.getProvider() + "/" + cfg.getModel());
        return cfg;
    }

    // ============ 行程推荐管理 ============

    public IPage<Itinerary> getItineraries(Integer recommended, String keyword, int page, int size) {
        QueryWrapper<Itinerary> qw = new QueryWrapper<>();
        if (recommended != null) qw.eq("is_recommended", recommended);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            if (kw.length() > 50) throw BusinessException.badRequest("搜索关键词最长 50 字");
            List<Long> authorIds = userMapper.selectList(
                    new QueryWrapper<User>().like("nickname", kw).select("id"))
                    .stream().map(User::getId).toList();
            qw.and(w -> {
                w.like("title", kw);
                if (!authorIds.isEmpty()) w.or().in("user_id", authorIds);
            });
        }
        // 已推荐行程优先，再按创建时间倒序
        qw.orderByDesc("is_recommended").orderByDesc("created_at");
        IPage<Itinerary> result = itineraryMapper.selectPage(PageUtil.of(page, size), qw);
        result.getRecords().forEach(r -> {
            r.setInputJson(null);
            r.setResultJson(null);
        });
        fillItineraryAuthorName(result.getRecords());
        return result;
    }

    private void fillItineraryAuthorName(List<Itinerary> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> userIds = records.stream().map(Itinerary::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) return;
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (Itinerary r : records) {
            User u = users.get(r.getUserId());
            if (u != null) r.setAuthorName(u.getNickname());
        }
    }

    public Itinerary recommendItinerary(Long id, Integer recommended, String reason,
                                        Long adminId, String role) {
        if (recommended == null || (recommended != 0 && recommended != 1)) {
            throw BusinessException.badRequest("推荐状态不合法（0=取消推荐 1=推荐）");
        }
        Itinerary it = itineraryMapper.selectById(id);
        if (it == null) throw BusinessException.notFound("行程不存在");
        UpdateWrapper<Itinerary> uw = new UpdateWrapper<>();
        uw.eq("id", id).set("is_recommended", recommended);
        if (recommended == 1) {
            uw.set("recommend_reason", reason == null || reason.isBlank() ? null : reason.trim());
        } else {
            uw.set("recommend_reason", null);
        }
        itineraryMapper.update(null, uw);
        if (recommended == 1) {
            notifyRecommendation(it, adminId);
        }
        writeLog(adminId, role, "status", "itinerary", id,
                recommended == 1 ? "推荐首页" : "取消推荐");
        return itineraryMapper.selectById(id);
    }

    /** 行程被推荐 → 通知行程作者 */
    private void notifyRecommendation(Itinerary it, Long fromUserId) {
        if (it.getUserId() == null) return;
        Notification n = new Notification();
        n.setUserId(it.getUserId());
        n.setType("recommend");
        n.setTitle("行程被推荐提醒");
        n.setContent("你的行程「" + truncate(it.getTitle(), 50) + "」已被管理员推荐到首页");
        n.setItineraryId(it.getId());
        n.setFromUserId(fromUserId);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ============ 操作日志 ============

    public IPage<OperationLog> getLogs(String targetType, int page, int size) {
        QueryWrapper<OperationLog> qw = new QueryWrapper<>();
        if (targetType != null && !targetType.isBlank()) qw.eq("target_type", targetType);
        qw.orderByDesc("created_at");
        return logMapper.selectPage(PageUtil.of(page, size), qw);
    }

    private void writeLog(Long adminId, String role, String action, String targetType,
                          Long targetId, String reason) {
        OperationLog entry = new OperationLog();
        entry.setOperatorId(adminId);
        entry.setOperatorRole(role);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setReason(reason);
        logMapper.insert(entry);
    }

    // ============ 仪表盘 ============

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("totalPosts", postMapper.selectCount(null));
        stats.put("totalCities", cityMapper.selectCount(null));

        long todayUsers = userMapper.selectCount(
                new QueryWrapper<User>().apply("DATE(created_at) = CURDATE()"));
        long todayPosts = postMapper.selectCount(
                new QueryWrapper<ForumPost>().apply("DATE(created_at) = CURDATE()"));
        stats.put("todayNew", todayUsers + todayPosts);

        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            String dateStr = day.format(fmt);
            Map<String, Object> row = new HashMap<>();
            row.put("date", dateStr);
            row.put("users", userMapper.selectCount(new QueryWrapper<User>()
                    .apply("DATE(created_at) = {0}", dateStr)));
            row.put("posts", postMapper.selectCount(new QueryWrapper<ForumPost>()
                    .apply("DATE(created_at) = {0}", dateStr)));
            trend.add(row);
        }
        stats.put("trend", trend);
        return stats;
    }
}
