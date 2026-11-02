package com.travelmate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.common.BusinessException;
import com.travelmate.common.PageResult;
import com.travelmate.common.PageUtil;
import com.travelmate.dto.ItinerarySaveDTO;
import com.travelmate.entity.ForumPost;
import com.travelmate.entity.Itinerary;
import com.travelmate.mapper.ForumPostMapper;
import com.travelmate.mapper.ItineraryMapper;
import lombok.RequiredArgsConstructor;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI 行程服务 — 保存/列表/详情/删除/复制收藏/推荐
 */
@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryMapper itineraryMapper;
    private final ForumPostMapper forumPostMapper;
    private final ObjectMapper objectMapper;

    public Itinerary save(Long userId, ItinerarySaveDTO dto) {
        Map<String, Object> result = dto.getResult();
        Map<?, ?> summary = result.get("summary") instanceof Map ? (Map<?, ?>) result.get("summary") : Map.of();
        Object titleObj = summary.get("title");
        String title = (dto.getTitle() == null || dto.getTitle().isBlank())
                ? (titleObj == null ? "我的行程" : String.valueOf(titleObj))
                : dto.getTitle();

        Itinerary it = new Itinerary();
        it.setUserId(userId);
        it.setTitle(title);
        it.setInputJson(writeJson(dto.getInput()));
        it.setResultJson(writeJson(result));
        it.setTotalDays(intOf(summary.get("totalDays")));
        it.setTotalBudget(decimalOf(summary.get("totalBudget")));
        it.setCopyFromId(null);
        it.setIsRecommended(0);
        itineraryMapper.insert(it);
        return it;
    }

    public PageResult<Itinerary> listMine(Long userId, int page, int pageSize) {
        return listByUser(userId, page, pageSize, false);
    }

    public PageResult<Itinerary> listFavorites(Long userId, int page, int pageSize) {
        return listByUser(userId, page, pageSize, true);
    }

    private PageResult<Itinerary> listByUser(Long userId, int page, int pageSize, boolean favoriteOnly) {
        QueryWrapper<Itinerary> qw = new QueryWrapper<Itinerary>().eq("user_id", userId);
        if (favoriteOnly) {
            qw.isNotNull("copy_from_id");
        } else {
            qw.isNull("copy_from_id");
        }
        qw.orderByDesc("created_at");
        Page<Itinerary> p = itineraryMapper.selectPage(PageUtil.of(page, pageSize), qw);
        p.getRecords().forEach(r -> {
            r.setInputJson(null);
            r.setResultJson(null);
        });
        return PageResult.of(p);
    }

    public Itinerary detail(Long userId, Long id) {
        Itinerary it = itineraryMapper.selectById(id);
        if (it == null) throw BusinessException.notFound("行程不存在");
        boolean owner = it.getUserId().equals(userId);
        if (!owner && !isShared(it)) throw BusinessException.forbidden("无权查看该行程");
        if (!owner) it.setInputJson(null);
        return it;
    }

    public void delete(Long userId, Long id) {
        Itinerary it = itineraryMapper.selectById(id);
        if (it == null) throw BusinessException.notFound("行程不存在");
        if (!it.getUserId().equals(userId)) throw BusinessException.forbidden("无权删除该行程");
        itineraryMapper.deleteById(id);
    }

    public Itinerary copy(Long userId, Long id) {
        Itinerary src = itineraryMapper.selectById(id);
        if (src == null) throw BusinessException.notFound("行程不存在");
        if (src.getUserId().equals(userId)) throw BusinessException.badRequest("不能收藏自己的行程");
        if (!isShared(src)) throw BusinessException.forbidden("该行程未公开分享，无法收藏");

        Itinerary it = new Itinerary();
        it.setUserId(userId);
        it.setTitle(src.getTitle());
        it.setInputJson(src.getInputJson());
        it.setResultJson(src.getResultJson());
        it.setTotalDays(src.getTotalDays());
        it.setTotalBudget(src.getTotalBudget());
        it.setCopyFromId(src.getId());
        it.setIsRecommended(0);
        itineraryMapper.insert(it);
        return it;
    }

    public PageResult<Itinerary> listRecommended(int page, int pageSize) {
        Page<Itinerary> p = itineraryMapper.selectPage(
                PageUtil.of(page, pageSize),
                new QueryWrapper<Itinerary>().eq("is_recommended", 1).orderByDesc("updated_at"));
        p.getRecords().forEach(r -> r.setInputJson(null));
        return PageResult.of(p);
    }

    /** 已分享 = 管理员推荐 或 被挂载到某个论坛帖子 */
    private boolean isShared(Itinerary it) {
        if (it.getIsRecommended() != null && it.getIsRecommended() == 1) return true;
        return forumPostMapper.selectCount(new QueryWrapper<ForumPost>().eq("itinerary_id", it.getId())) > 0;
    }

    /** 导出简化 PDF（仅本人） */
    public byte[] exportPdf(Long userId, Long id) {
        Itinerary it = itineraryMapper.selectById(id);
        if (it == null) throw BusinessException.notFound("行程不存在");
        if (!it.getUserId().equals(userId)) throw BusinessException.forbidden("无权导出该行程");
        Map<String, Object> result = parseJson(it.getResultJson());
        return renderPdf(buildPdfText(it.getTitle(), result));
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException(500, "行程数据解析失败");
        }
    }

    private String buildPdfText(String title, Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> summary = asMap(result.get("summary"));
        sb.append("行程：").append(title).append("\n");
        if (summary.get("headline") != null) sb.append(summary.get("headline")).append("\n");
        sb.append("总天数：").append(summary.get("totalDays")).append(" 天    总预算：¥")
          .append(summary.get("totalBudget")).append("\n\n");

        for (Map<String, Object> day : asList(result.get("days"))) {
            sb.append("【第").append(day.get("day")).append("天");
            if (day.get("date") != null) sb.append(" ").append(day.get("date"));
            if (day.get("city") != null) sb.append(" · ").append(day.get("city"));
            if (day.get("theme") != null) sb.append(" · ").append(day.get("theme"));
            sb.append("】\n");
            for (Map<String, Object> item : asList(day.get("items"))) {
                sb.append("  ").append(item.get("time")).append(" [").append(typeLabel(item.get("type")))
                  .append("] ").append(item.get("name"));
                if (item.get("price") != null) sb.append(" ¥").append(item.get("price"));
                sb.append("\n");
                Object note = item.get("note");
                if (note != null && !String.valueOf(note).isBlank()) {
                    sb.append("       ").append(note).append("\n");
                }
            }
            sb.append("\n");
        }

        Map<String, Object> cost = asMap(result.get("costBreakdown"));
        if (!cost.isEmpty()) {
            sb.append("【费用明细】\n");
            sb.append("交通：¥").append(cost.get("transport")).append("\n");
            sb.append("住宿：¥").append(cost.get("accommodation")).append("\n");
            sb.append("餐饮：¥").append(cost.get("meals")).append("\n");
            sb.append("门票：¥").append(cost.get("tickets")).append("\n");
            sb.append("其他：¥").append(cost.get("other")).append("\n");
            sb.append("合计：¥").append(cost.get("total")).append("\n\n");
        }

        List<?> tips = result.get("tips") instanceof List ? (List<?>) result.get("tips") : List.of();
        if (!tips.isEmpty()) {
            sb.append("【小贴士】\n");
            for (Object t : tips) sb.append("- ").append(t).append("\n");
        }
        sb.append("\n注：以上价格为 AI 估算，仅供参考，实际费用可能与估算略有出入（可略高或略低）。\n");
        return sb.toString();
    }

    private byte[] renderPdf(String content) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDFont font = loadFont(doc);
            float leading = 16f;
            float margin = 50f;
            PDPage page = new PDPage();
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - margin;
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                cs.setFont(font, 12);
                for (String line : content.split("\n", -1)) {
                    if (y < margin) {
                        cs.close();
                        page = new PDPage();
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(font, 12);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    int codePoints = line.codePointCount(0, line.length());
                    cs.showText(codePoints > 100 ? line.substring(0, line.offsetByCodePoints(0, 100)) : line);
                    cs.endText();
                    y -= leading;
                }
            } finally {
                cs.close();
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw BusinessException.badRequest("生成 PDF 失败：" + e.getMessage());
        }
    }

    private PDFont loadFont(PDDocument doc) throws IOException {
        File simsun = new File("C:/Windows/Fonts/simsun.ttc");
        if (simsun.exists()) {
            try (TrueTypeCollection ttc = new TrueTypeCollection(simsun)) {
                TrueTypeFont ttf = ttc.getFontByName("SimSun");
                if (ttf != null) return PDType0Font.load(doc, ttf, true);
            } catch (Exception ignored) { }
        }
        File simhei = new File("C:/Windows/Fonts/simhei.ttf");
        if (simhei.exists()) {
            try {
                return PDType0Font.load(doc, simhei);
            } catch (Exception ignored) { }
        }
        return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : List.of();
    }

    private String typeLabel(Object t) {
        if (t == null) return "其他";
        return switch (String.valueOf(t)) {
            case "attraction" -> "景点";
            case "meal" -> "餐饮";
            case "hotel" -> "酒店";
            case "transport" -> "交通";
            default -> "其他";
        };
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new BusinessException(500, "数据序列化失败");
        }
    }

    private int intOf(Object v) {
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private BigDecimal decimalOf(Object v) {
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return BigDecimal.ZERO;
    }
}
