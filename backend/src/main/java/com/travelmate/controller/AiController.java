package com.travelmate.controller;

import com.travelmate.common.Result;
import com.travelmate.dto.AiGenerateDTO;
import com.travelmate.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 行程生成控制器
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@Valid @RequestBody AiGenerateDTO dto) {
        return Result.success(aiService.generate(dto));
    }
}
