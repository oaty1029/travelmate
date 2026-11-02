package com.travelmate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmate.common.BusinessException;
import com.travelmate.dto.AiGenerateDTO;
import com.travelmate.entity.AiConfig;
import com.travelmate.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 行程生成服务 — 调用 DeepSeek，校验返回 JSON 并重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiConfigMapper aiConfigMapper;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一名专业的旅游行程规划师。请根据用户提供的出行信息，生成一份完整、可执行的旅游行程。

            必须严格按以下 JSON 格式返回，不要输出任何多余文字，只输出 JSON：

            {
              "summary": {
                "title": "行程标题",
                "totalDays": 3,
                "totalBudget": 5000,
                "headline": "一句话亮点"
              },
              "days": [
                {
                  "day": 1,
                  "date": "2026-10-01",
                  "city": "北京",
                  "theme": "当天主题",
                  "items": [
                    {
                      "time": "09:00-11:30",
                      "type": "attraction",
                      "name": "故宫博物院",
                      "note": "推荐理由或预约提示",
                      "price": 60,
                      "link": "可选：第三方链接"
                    }
                  ]
                }
              ],
              "costBreakdown": {
                "transport": 1200,
                "accommodation": 1800,
                "meals": 900,
                "tickets": 400,
                "other": 500,
                "total": 4800
              },
              "tips": ["注意事项1", "注意事项2"]
            }

            规则：
            1. days 按天排列，每天 items 按时间顺序；type 取值：attraction(景点)、meal(餐饮)、hotel(住宿)、transport(交通)。
            2. 每天应覆盖景点、餐饮、住宿；跨城市当天应包含交通。
            3. price 为该条目的预估价格（元），需考虑节假日/旺季价格上浮、景区活动等因素。
            4. summary.totalBudget 与 costBreakdown.total 必须一致，且约等于「人均预算 × 出行人数」（可略高或略低）。
            5. 多城市行程按地理位置合理串联，避免来回折返。
            6. 金额一律用数字，不要带"元"或千分位。
            7. 所有价格均为 AI 估算值，允许与实际略有出入，但需保持在合理范围内。
            8. costBreakdown 各分项（transport/accommodation/meals/tickets/other）相加必须等于 total。
            """;

    public Map<String, Object> generate(AiGenerateDTO input) {
        AiConfig cfg = requireConfig();
        String userPrompt = buildUserPrompt(input);
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String content = callDeepSeek(cfg, userPrompt);
                Map<String, Object> result = objectMapper.readValue(
                        stripCodeFence(content), new TypeReference<Map<String, Object>>() {});
                validateItinerary(result, input);
                return result;
            } catch (Exception e) {
                log.warn("AI 行程生成第 {} 次失败: {}", attempt, e.getMessage());
            }
        }
        throw new BusinessException(500, "AI 生成失败，请稍后重试");
    }

    private AiConfig requireConfig() {
        AiConfig cfg = aiConfigMapper.selectOne(new QueryWrapper<AiConfig>().last("LIMIT 1"));
        if (cfg == null || cfg.getEnabled() == null || cfg.getEnabled() != 1) {
            throw BusinessException.badRequest("AI 服务未启用，请先在管理后台配置");
        }
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw BusinessException.badRequest("AI 服务未配置 API Key，请先在管理后台配置");
        }
        return cfg;
    }

    private String callDeepSeek(AiConfig cfg, String userPrompt) {
        String base = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl().replaceAll("/+$", "");
        String url = base + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", cfg.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("temperature", cfg.getTemperature() != null ? cfg.getTemperature() : BigDecimal.valueOf(1.0));
        body.put("max_tokens", cfg.getMaxTokens() != null ? cfg.getMaxTokens() : 4096);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cfg.getApiKey());

        RestTemplate rt = new RestTemplate();
        ResponseEntity<Map> resp = rt.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        if (resp.getBody() == null) throw new BusinessException("AI 返回为空");

        Object choicesObj = resp.getBody().get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new BusinessException("AI 返回缺少 choices");
        }
        Object messageObj = ((Map<?, ?>) choices.get(0)).get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            throw new BusinessException("AI 返回缺少 message");
        }
        Object content = message.get("content");
        if (content == null) throw new BusinessException("AI 返回缺少 content");
        return content.toString();
    }

    private String stripCodeFence(String content) {
        String s = content.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return s;
    }

    private void validateItinerary(Map<String, Object> it, AiGenerateDTO input) {
        if (!(it.get("summary") instanceof Map<?, ?> summary)) throw new BusinessException("AI 返回缺少 summary");
        if (!(it.get("days") instanceof List<?> days)) throw new BusinessException("AI 返回缺少 days");
        if (!(it.get("costBreakdown") instanceof Map<?, ?> cost)) throw new BusinessException("AI 返回缺少 costBreakdown");
        if (!(it.get("tips") instanceof List)) throw new BusinessException("AI 返回缺少 tips");
        if (days.isEmpty()) throw new BusinessException("AI 返回 days 为空");
        for (Object d : days) {
            if (!(d instanceof Map<?, ?> day)) throw new BusinessException("days 元素格式错误");
            if (!(day.get("items") instanceof List<?> items)) throw new BusinessException("day 缺少 items");
            for (Object item : items) {
                if (item instanceof Map<?, ?> m) {
                    BigDecimal price = numOf(m.get("price"));
                    if (price != null && price.signum() < 0) throw new BusinessException("条目价格不能为负");
                }
            }
        }

        // 数值一致性校验：分项和 == total == totalBudget，且总预算落在人均预算的合理带内
        BigDecimal totalBudget = numOf(summary.get("totalBudget"));
        BigDecimal cbTotal = numOf(cost.get("total"));
        if (totalBudget == null || totalBudget.signum() < 0) throw new BusinessException("总预算数据不合法");
        if (cbTotal == null || cbTotal.signum() < 0) throw new BusinessException("费用明细 total 不合法");

        BigDecimal sum = BigDecimal.ZERO;
        for (String key : List.of("transport", "accommodation", "meals", "tickets", "other")) {
            BigDecimal v = numOf(cost.get(key));
            if (v != null) {
                if (v.signum() < 0) throw new BusinessException("费用分项不能为负");
                sum = sum.add(v);
            }
        }
        if (differs(sum, cbTotal, toleranceOf(cbTotal))) {
            throw new BusinessException("费用分项合计与 total 不一致");
        }
        if (differs(totalBudget, cbTotal, toleranceOf(cbTotal))) {
            throw new BusinessException("总预算与费用明细不一致");
        }
        if (input.getBudgetPerPerson() != null && input.getPeople() != null) {
            BigDecimal expected = input.getBudgetPerPerson()
                    .multiply(BigDecimal.valueOf(input.getPeople()));
            if (expected.signum() > 0) {
                BigDecimal low = expected.multiply(new BigDecimal("0.3"));
                BigDecimal high = expected.multiply(new BigDecimal("3"));
                if (totalBudget.compareTo(low) < 0 || totalBudget.compareTo(high) > 0) {
                    throw new BusinessException("总预算偏离人均预算过大");
                }
            }
        }
    }

    private BigDecimal numOf(Object v) {
        if (v instanceof Number n) return new BigDecimal(n.toString());
        return null;
    }

    private BigDecimal toleranceOf(BigDecimal v) {
        return v.abs().multiply(new BigDecimal("0.01")).max(BigDecimal.ONE);
    }

    private boolean differs(BigDecimal a, BigDecimal b, BigDecimal tol) {
        return a.subtract(b).abs().compareTo(tol) > 0;
    }

    private String buildUserPrompt(AiGenerateDTO in) {
        StringBuilder sb = new StringBuilder("请根据以下信息规划一份完整旅游行程：\n");
        sb.append("- 出发地：").append(in.getDeparture()).append("\n");
        sb.append("- 目的地：").append(String.join("、", in.getDestinations())).append("\n");
        sb.append("- 出发日期：").append(in.getStartDate()).append("\n");
        sb.append("- 游玩天数：").append(in.getDays()).append(" 天\n");
        sb.append("- 出行人数：").append(in.getPeople()).append(" 人\n");
        sb.append("- 人均预算：").append(in.getBudgetPerPerson()).append(" 元（").append(in.getBudgetLevel()).append("档）\n");
        sb.append("- 酒店偏好：").append(in.getHotelLevel()).append("\n");
        if (in.getCompanions() != null && !in.getCompanions().isEmpty())
            sb.append("- 同行人：").append(String.join("、", in.getCompanions())).append("\n");
        if (in.getInterests() != null && !in.getInterests().isEmpty())
            sb.append("- 兴趣偏好：").append(String.join("、", in.getInterests())).append("\n");
        if (in.getTransport() != null && !in.getTransport().isBlank())
            sb.append("- 出行方式：").append(in.getTransport()).append("\n");
        if (in.getDiet() != null && !in.getDiet().isBlank())
            sb.append("- 餐饮偏好：").append(in.getDiet()).append("\n");
        if (in.getPace() != null && !in.getPace().isBlank())
            sb.append("- 行程节奏：").append(in.getPace()).append("\n");
        return sb.toString();
    }
}
