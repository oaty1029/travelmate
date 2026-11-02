package com.travelmate.controller;

import com.travelmate.common.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 健康检查控制器 - 公开放行
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Result<String> health() {
        return Result.success("ok");
    }
}
