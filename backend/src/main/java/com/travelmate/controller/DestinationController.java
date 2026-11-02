package com.travelmate.controller;

import com.travelmate.common.PageResult;
import com.travelmate.common.Result;
import com.travelmate.dto.PageQuery;
import com.travelmate.entity.City;
import com.travelmate.entity.Province;
import com.travelmate.service.DestinationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 目的地控制器 - 全部公开放行
 */
@RestController
@RequestMapping("/api/v1/destination")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping("/provinces")
    public Result<List<Province>> getProvinces() {
        return Result.success(destinationService.getProvinces());
    }

    @GetMapping("/cities")
    public Result<PageResult<City>> getCities(@RequestParam(required = false) Long provinceId,
                                              @Valid PageQuery query) {
        return Result.success(destinationService.getCities(provinceId, query.getPage(), query.getPageSize()));
    }

    @GetMapping("/cities/popular")
    public Result<List<City>> getPopularCities(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(destinationService.getPopularCities(limit));
    }

    @GetMapping("/cities/{cityId}")
    public Result<Map<String, Object>> getCityDetail(@PathVariable Long cityId) {
        return Result.success(destinationService.getCityDetail(cityId));
    }

    @GetMapping("/cities/search")
    public Result<List<City>> searchCities(@RequestParam String keyword) {
        return Result.success(destinationService.searchCities(keyword));
    }

    @GetMapping("/resolve")
    public Result<Map<String, Object>> resolveKeyword(@RequestParam String keyword) {
        return Result.success(destinationService.resolveKeyword(keyword));
    }
}
