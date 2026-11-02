package com.travelmate.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.travelmate.common.BusinessException;
import com.travelmate.common.PageResult;
import com.travelmate.common.PageUtil;
import com.travelmate.entity.Attraction;
import com.travelmate.entity.City;
import com.travelmate.entity.Province;
import com.travelmate.mapper.AttractionMapper;
import com.travelmate.mapper.CityMapper;
import com.travelmate.mapper.ProvinceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 目的地服务 - 省份/城市 查询
 */
@Service
@RequiredArgsConstructor
public class DestinationService {

    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    private final AttractionMapper attractionMapper;

    private static final int SEARCH_KEYWORD_MAX = 50;

    @Cacheable(cacheNames = "provinces")
    public List<Province> getProvinces() {
        return provinceMapper.selectList(
                new QueryWrapper<Province>().orderByAsc("sort_order")
        );
    }

    public PageResult<City> getCities(Long provinceId, int page, int pageSize) {
        QueryWrapper<City> wrapper = new QueryWrapper<City>()
                .eq("status", 1)
                .orderByDesc("popularity");
        if (provinceId != null) {
            wrapper.eq("province_id", provinceId);
        }
        IPage<City> result = cityMapper.selectPage(PageUtil.of(page, pageSize), wrapper);
        fillProvinceNames(result.getRecords());
        return PageResult.of(result);
    }

    /** 城市详情（含附属景点列表） */
    public Map<String, Object> getCityDetail(Long cityId) {
        City city = cityMapper.selectById(cityId);
        if (city == null || city.getStatus() != 1) {
            throw BusinessException.notFound("城市不存在");
        }
        fillProvinceNames(List.of(city));
        List<Attraction> attractions = attractionMapper.selectList(
                new QueryWrapper<Attraction>()
                        .eq("city_id", cityId)
                        .eq("status", 1)
                        .orderByDesc("rating")
                        .orderByAsc("id")
        );
        Map<String, Object> result = new HashMap<>();
        result.put("city", city);
        result.put("attractions", attractions);
        return result;
    }

    /** 解析地名关键词：用于出发地/目的地逻辑校验，返回 type=city/province/unknown */
    public Map<String, Object> resolveKeyword(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", kw);
        result.put("type", "unknown");
        result.put("province", null);
        result.put("cities", List.of());

        if (kw.isEmpty()) {
            return result;
        }

        // 1) 城市精确匹配优先（直辖市如"北京/上海"既是省又是市，按城市处理）
        City city = matchCity(kw);
        if (city != null) {
            fillProvinceNames(List.of(city));
            result.put("type", "city");
            result.put("cities", List.of(city));
            return result;
        }

        // 2) 省份匹配（支持简称/全称/去后缀，如 江西/江西省）
        Province province = matchProvince(kw);
        if (province != null) {
            List<City> cities = cityMapper.selectList(
                    new QueryWrapper<City>()
                            .eq("province_id", province.getId())
                            .eq("status", 1)
                            .orderByDesc("popularity"));
            fillProvinceNames(cities);
            result.put("type", "province");
            result.put("province", province);
            result.put("cities", cities);
            return result;
        }

        // 3) 模糊建议（省份/城市名称或拼音包含关键词）
        result.put("provinces", provinceMapper.selectList(
                new QueryWrapper<Province>().like("name", kw).last("LIMIT 10")));
        List<City> suggestions = cityMapper.selectList(
                new QueryWrapper<City>()
                        .eq("status", 1)
                        .and(w -> w.like("name", kw).or().like("pinyin", kw.toLowerCase()))
                        .orderByDesc("popularity")
                        .last("LIMIT 10"));
        fillProvinceNames(suggestions);
        result.put("cities", suggestions);
        return result;
    }

    private City matchCity(String kw) {
        City city = firstCity(new QueryWrapper<City>().eq("status", 1).eq("name", kw));
        if (city != null) return city;
        city = firstCity(new QueryWrapper<City>().eq("status", 1).eq("pinyin", kw.toLowerCase()));
        if (city != null) return city;
        // 处理全称如"上海市" → 简称"上海"
        String shortKw = stripRegionSuffix(kw);
        if (!shortKw.equals(kw)) {
            return firstCity(new QueryWrapper<City>().eq("status", 1).eq("name", shortKw));
        }
        return null;
    }

    private City firstCity(QueryWrapper<City> wrapper) {
        List<City> list = cityMapper.selectList(wrapper.last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private Province matchProvince(String kw) {
        String norm = stripRegionSuffix(kw);
        for (Province p : provinceMapper.selectList(null)) {
            if (p.getName().equals(kw) || stripRegionSuffix(p.getName()).equals(norm)) {
                return p;
            }
        }
        return null;
    }

    private String stripRegionSuffix(String s) {
        if (s == null) return "";
        String[] suffixes = {"壮族自治区", "回族自治区", "维吾尔自治区", "特别行政区", "自治区", "省", "市"};
        for (String suf : suffixes) {
            if (s.endsWith(suf)) return s.substring(0, s.length() - suf.length());
        }
        return s;
    }

    /** 关键词搜索城市 */
    public List<City> searchCities(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            throw BusinessException.badRequest("搜索关键词不能为空");
        }
        if (kw.length() > SEARCH_KEYWORD_MAX) {
            throw BusinessException.badRequest("搜索关键词过长（最多" + SEARCH_KEYWORD_MAX + "个字符）");
        }
        List<City> cities = cityMapper.selectList(
                new QueryWrapper<City>()
                        .eq("status", 1)
                        .and(w -> w.like("name", kw).or().like("pinyin", kw))
                        .orderByDesc("popularity")
                        .last("LIMIT 30")
        );
        fillProvinceNames(cities);
        return cities;
    }

    /** 热门城市（管理员设为热门优先，其次按热度补足） */
    public List<City> getPopularCities(int limit) {
        List<City> cities = cityMapper.selectList(
                new QueryWrapper<City>()
                        .eq("status", 1)
                        .orderByDesc("is_popular")
                        .orderByDesc("popularity")
                        .last("LIMIT " + Math.max(1, Math.min(limit, 100)))
        );
        fillProvinceNames(cities);
        return cities;
    }

    /** 批量填充城市的省份名 */
    private void fillProvinceNames(List<City> cities) {
        Set<Long> ids = cities.stream()
                .map(City::getProvinceId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return;
        Map<Long, String> nameMap = provinceMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Province::getId, Province::getName, (a, b) -> a));
        cities.forEach(c -> c.setProvinceName(nameMap.get(c.getProvinceId())));
    }
}
