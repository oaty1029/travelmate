package com.travelmate.controller;

import com.travelmate.common.PageResult;
import com.travelmate.common.Result;
import com.travelmate.dto.ItinerarySaveDTO;
import com.travelmate.entity.Itinerary;
import com.travelmate.service.ItineraryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 行程控制器
 */
@RestController
@RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping
    public Result<Itinerary> save(@Valid @RequestBody ItinerarySaveDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(itineraryService.save(userId, dto));
    }

    @GetMapping
    public Result<PageResult<Itinerary>> listMine(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(itineraryService.listMine(userId, page, pageSize));
    }

    @GetMapping("/favorites")
    public Result<PageResult<Itinerary>> listFavorites(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize,
                                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(itineraryService.listFavorites(userId, page, pageSize));
    }

    @GetMapping("/recommended")
    public Result<PageResult<Itinerary>> recommended(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(itineraryService.listRecommended(page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Itinerary> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(itineraryService.detail(userId, id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        itineraryService.delete(userId, id);
        return Result.success();
    }

    @PostMapping("/{id}/copy")
    public Result<Itinerary> copy(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return Result.success(itineraryService.copy(userId, id));
    }

    @GetMapping(value = "/{id}/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        byte[] pdf = itineraryService.exportPdf(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"itinerary-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
