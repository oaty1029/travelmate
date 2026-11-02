package com.travelmate.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travelmate.entity.*;
import com.travelmate.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 种子数据初始化 - 启动时自动填充省份/城市/管理员数据
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SeedRunner implements CommandLineRunner {

    private final ProvinceMapper provinceMapper;
    private final CityMapper cityMapper;
    private final UserMapper userMapper;
    private final ForumCategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedProvinces();
        seedCities();
        seedAdminUser();
        seedForumCategories();
    }

    // ================ 省份 ================

    private void seedProvinces() {
        if (provinceMapper.selectCount(null) > 0) return;
        String[][] provinces = {
                // { name, region, pinyin, sortOrder }
                {"北京市", "华北", "beijing", "1"}, {"天津市", "华北", "tianjin", "2"},
                {"河北省", "华北", "hebei", "3"}, {"山西省", "华北", "shanxi", "4"},
                {"内蒙古自治区", "华北", "neimenggu", "5"},
                {"辽宁省", "东北", "liaoning", "6"}, {"吉林省", "东北", "jilin", "7"},
                {"黑龙江省", "东北", "heilongjiang", "8"},
                {"上海市", "华东", "shanghai", "9"}, {"江苏省", "华东", "jiangsu", "10"},
                {"浙江省", "华东", "zhejiang", "11"}, {"安徽省", "华东", "anhui", "12"},
                {"福建省", "华东", "fujian", "13"}, {"江西省", "华东", "jiangxi", "14"},
                {"山东省", "华东", "shandong", "15"},
                {"河南省", "中南", "henan", "16"}, {"湖北省", "中南", "hubei", "17"},
                {"湖南省", "中南", "hunan", "18"}, {"广东省", "中南", "guangdong", "19"},
                {"广西壮族自治区", "中南", "guangxi", "20"}, {"海南省", "中南", "hainan", "21"},
                {"重庆市", "西南", "chongqing", "22"}, {"四川省", "西南", "sichuan", "23"},
                {"贵州省", "西南", "guizhou", "24"}, {"云南省", "西南", "yunnan", "25"},
                {"西藏自治区", "西南", "xizang", "26"},
                {"陕西省", "西北", "shaanxi", "27"}, {"甘肃省", "西北", "gansu", "28"},
                {"青海省", "西北", "qinghai", "29"}, {"宁夏回族自治区", "西北", "ningxia", "30"},
                {"新疆维吾尔自治区", "西北", "xinjiang", "31"},
                {"台湾省", "港澳台", "taiwan", "32"}, {"香港特别行政区", "港澳台", "xianggang", "33"},
                {"澳门特别行政区", "港澳台", "aomen", "34"},
        };
        for (String[] p : provinces) {
            Province prov = new Province();
            prov.setName(p[0]);
            prov.setRegion(p[1]);
            prov.setPinyin(p[2]);
            prov.setSortOrder(Integer.parseInt(p[3]));
            provinceMapper.insert(prov);
        }
        log.info("Seeded {} provinces", provinces.length);
    }

    // ================ 城市 ================

    private void seedCities() {
        if (cityMapper.selectCount(null) > 0) return;
        City[] realCities = {
                buildCity("北京市", "北京", "beijing", "中国首都，历史文化名城", 5000, "39.9042", "116.4074", "4季", "历史,文化,美食,都市"),
                buildCity("上海市", "上海", "shanghai", "东方魔都，国际化大都市", 5000, "31.2304", "121.4737", "春秋", "都市,购物,美食"),
                buildCity("重庆市", "重庆", "chongqing", "山城，火锅之都", 4500, "29.4316", "106.9123", "春秋", "火锅,夜景"),
                buildCity("四川省", "成都", "chengdu", "天府之国，慢生活之城", 4500, "30.5728", "104.0668", "3-6,9-11月", "熊猫,美食,茶馆"),
                buildCity("陕西省", "西安", "xian", "十三朝古都", 3500, "34.3416", "108.9398", "春秋", "历史,古都,兵马俑"),
                buildCity("浙江省", "杭州", "hangzhou", "人间天堂，西湖之城", 3000, "30.2741", "120.1551", "春秋", "西湖,丝绸,美食"),
                buildCity("江苏省", "南京", "nanjing", "六朝古都", 2500, "32.0603", "118.7969", "春秋", "历史,古都,秦淮河"),
                buildCity("江苏省", "苏州", "suzhou", "园林之城", 2500, "31.2990", "120.5853", "春秋", "园林,古镇,丝绸"),
                buildCity("湖北省", "武汉", "wuhan", "九省通衢", 2500, "30.5928", "114.3055", "春秋", "黄鹤楼,热干面,长江"),
                buildCity("广东省", "广州", "guangzhou", "南国花城", 3000, "23.1291", "113.2644", "四季", "美食,早茶,小蛮腰"),
                buildCity("广东省", "深圳", "shenzhen", "创新之城", 2500, "22.5431", "114.0579", "四季", "科技,主题乐园"),
                buildCity("云南省", "昆明", "kunming", "春城，四季如春", 2500, "25.0389", "102.7183", "四季", "石林,滇池,过桥米线"),
                buildCity("云南省", "大理", "dali", "风花雪月之城", 2000, "25.6065", "100.2676", "四季", "洱海,苍山,古城"),
                buildCity("云南省", "丽江", "lijiang", "古城与雪山", 3000, "26.8721", "100.2299", "春秋", "古城,雪山,茶马古道"),
                buildCity("海南省", "三亚", "sanya", "东方夏威夷", 3500, "18.2528", "109.5119", "冬季", "海滩,度假,海鲜"),
                buildCity("山东省", "青岛", "qingdao", "海滨啤酒之城", 2500, "36.0671", "120.3826", "春夏秋", "啤酒,海鲜,红瓦绿树"),
                buildCity("福建省", "厦门", "xiamen", "海上花园", 2500, "24.4798", "118.0894", "秋冬", "鼓浪屿,海,沙茶面"),
                buildCity("天津市", "天津", "tianjin", "北方港口城市，曲艺之乡", 2000, "39.3434", "117.3616", "春秋", "港口,美食,曲艺"),
                buildCity("湖南省", "长沙", "changsha", "娱乐美食之都", 2500, "28.2282", "112.9388", "春秋", "美食,湖南卫视,橘子洲"),
                buildCity("湖南省", "张家界", "zhangjiajie", "世界自然遗产", 2000, "29.1291", "110.4792", "4-10月", "山水,阿凡达取景地"),
                buildCity("河南省", "洛阳", "luoyang", "九朝古都", 2000, "34.6197", "112.4540", "春秋", "龙门石窟,牡丹"),
                buildCity("河南省", "开封", "kaifeng", "七朝古都", 1500, "34.7972", "114.3074", "春秋", "清明上河园,菊花"),
                buildCity("四川省", "九寨沟", "jiuzhaigou", "人间仙境", 1500, "33.2600", "103.9200", "9-10月", "山水,彩色湖泊"),
                buildCity("四川省", "乐山", "leshan", "大佛之乡", 1500, "29.5455", "103.7661", "春秋", "大佛,峨眉山"),
                buildCity("广西壮族自治区", "桂林", "guilin", "山水甲天下", 2500, "25.2742", "110.2992", "4-10月", "漓江,山水"),
                buildCity("西藏自治区", "拉萨", "lasa", "日光城，朝圣之地", 2000, "29.6517", "91.1409", "5-10月", "布达拉宫,藏文化"),
                buildCity("新疆维吾尔自治区", "乌鲁木齐", "wulumuqi", "丝路重镇", 2000, "43.8256", "87.6168", "6-9月", "天山,大巴扎"),
                buildCity("台湾省", "台北", "taibei", "宝岛首都", 2000, "25.0330", "121.5654", "秋冬", "故宫101,夜市,美食"),
        };
        for (City c : realCities) cityMapper.insert(c);
        log.info("Seeded {} cities", realCities.length);
    }

    private City buildCity(String provinceName, String name, String pinyin, String desc,
                           int popularity, String lat, String lng, String best, String tags) {
        City c = new City();
        c.setName(name);
        c.setPinyin(pinyin);
        c.setDescription(desc);
        c.setPopularity(popularity);
        c.setBestSeason(best);
        c.setTags(tags != null ? "[\"" + tags.replace(",", "\",\"") + "\"]" : null);
        c.setStatus(1);
        if (lat != null) c.setLat(new BigDecimal(lat));
        if (lng != null) c.setLng(new BigDecimal(lng));
        Province p = provinceMapper.selectOne(new QueryWrapper<Province>().eq("name", provinceName));
        if (p != null) c.setProvinceId(p.getId());
        return c;
    }

    // ================ Admin 用户 ================

    private void seedAdminUser() {
        if (userMapper.selectCount(new QueryWrapper<User>().eq("email", "admin@travelmate.com")) > 0) return;
        User admin = new User();
        admin.setEmail("admin@travelmate.com");
        admin.setNickname("超级管理员");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole("admin");
        admin.setStatus(1);
        userMapper.insert(admin);
        log.info("Seeded admin user (admin@travelmate.com)");
    }

    // ================ 论坛分类 ================

    private void seedForumCategories() {
        if (categoryMapper.selectCount(null) > 0) return;
        String[][] cats = {
                {"游记分享", "分享你的旅行故事和精彩瞬间", "1"},
                {"攻略推荐", "实用攻略、行程规划", "2"},
                {"美食探店", "各地特色美食、餐厅推荐", "3"},
                {"摄影作品", "旅行中的精美照片", "4"},
                {"结伴同行", "找搭子一起出发", "5"},
                {"问答求助", "旅行问题求助", "6"},
        };
        for (String[] c : cats) {
            ForumCategory cat = new ForumCategory();
            cat.setName(c[0]);
            cat.setDescription(c[1]);
            cat.setSortOrder(Integer.parseInt(c[2]));
            categoryMapper.insert(cat);
        }
        log.info("Seeded {} forum categories", cats.length);
    }
}
