package com.travelmate.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页工具 — 统一 page/pageSize 边界收敛
 */
public final class PageUtil {

    private PageUtil() {}

    public static <T> Page<T> of(int page, int pageSize) {
        int p = Math.max(1, page);
        int s = Math.min(Math.max(1, pageSize), 100);
        return new Page<>(p, s);
    }
}
