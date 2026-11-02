-- =============================================================
-- 批3：基础数据扩充（城市 28→50、景点 72→220+、酒店数据）
-- 目标库：travelmate (MySQL 5.7)
-- 前置：需先执行 03-hotels-table.sql 建好 hotels 表
-- 说明：
--   1) 本脚本为增量脚本，可重复执行前请确认数据未重复（景点/酒店按城市名匹配 city_id）。
--   2) 景点评分差异化：现有景点先按 id 均匀铺开，再对知名景点覆盖高分，
--      使「体验最优按评分选点」真正生效。
-- =============================================================

USE travelmate;

-- ================= 一、新增城市（28 → 50） =================

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '哈尔滨', 'haerbin', '冰城，冰雪之都', 3000, '冬季', '["冰雪","俄式建筑","中央大街"]', 45.8038, 126.5349, 1, NOW(), NOW() FROM provinces p WHERE p.name = '黑龙江省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '长春', 'changchun', '北国春城，电影之都', 2000, '春秋', '["伪满","电影","冰雪"]', 43.8171, 125.3235, 1, NOW(), NOW() FROM provinces p WHERE p.name = '吉林省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '沈阳', 'shenyang', '盛京古城，工业重镇', 2500, '春秋', '["故宫","历史","工业"]', 41.8057, 123.4315, 1, NOW(), NOW() FROM provinces p WHERE p.name = '辽宁省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '大连', 'dalian', '浪漫海滨之都', 2800, '夏季', '["海滨","海鲜","广场"]', 38.9140, 121.6147, 1, NOW(), NOW() FROM provinces p WHERE p.name = '辽宁省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '郑州', 'zhengzhou', '中原枢纽，商都', 2000, '春秋', '["少林","黄河","历史"]', 34.7466, 113.6254, 1, NOW(), NOW() FROM provinces p WHERE p.name = '河南省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '济南', 'jinan', '泉城，家家泉水', 2200, '春秋', '["泉水","历史","大明湖"]', 36.6512, 117.1201, 1, NOW(), NOW() FROM provinces p WHERE p.name = '山东省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '石家庄', 'shijiazhuang', '华北重镇，红色圣地', 1200, '春秋', '["古城","红色","太行"]', 38.0428, 114.5149, 1, NOW(), NOW() FROM provinces p WHERE p.name = '河北省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '太原', 'taiyuan', '龙城，晋商故里', 1500, '春秋', '["晋祠","古建","面食"]', 37.8706, 112.5489, 1, NOW(), NOW() FROM provinces p WHERE p.name = '山西省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '呼和浩特', 'huhehaote', '青城，草原之都', 1500, '夏季', '["草原","蒙古","大召"]', 40.8424, 111.7490, 1, NOW(), NOW() FROM provinces p WHERE p.name = '内蒙古自治区';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '福州', 'fuzhou', '榕城，温泉之乡', 2000, '秋冬', '["三坊七巷","温泉","茉莉"]', 26.0745, 119.2965, 1, NOW(), NOW() FROM provinces p WHERE p.name = '福建省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '南昌', 'nanchang', '英雄城，军旗升起的地方', 1800, '春秋', '["滕王阁","红色","米粉"]', 28.6820, 115.8579, 1, NOW(), NOW() FROM provinces p WHERE p.name = '江西省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '合肥', 'hefei', '庐州，科教名城', 1500, '春秋', '["古镇","科技","包公"]', 31.8206, 117.2272, 1, NOW(), NOW() FROM provinces p WHERE p.name = '安徽省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '南宁', 'nanning', '绿城，东盟之窗', 1800, '四季', '["青秀山","东盟","米粉"]', 22.8170, 108.3665, 1, NOW(), NOW() FROM provinces p WHERE p.name = '广西壮族自治区';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '贵阳', 'guiyang', '避暑之都，爽爽贵阳', 1800, '夏季', '["山水","酸汤","青岩"]', 26.6470, 106.6302, 1, NOW(), NOW() FROM provinces p WHERE p.name = '贵州省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '兰州', 'lanzhou', '黄河之都，牛肉面故乡', 1800, '春秋', '["黄河","牛肉面","铁桥"]', 36.0611, 103.8343, 1, NOW(), NOW() FROM provinces p WHERE p.name = '甘肃省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '西宁', 'xining', '高原夏都，青藏门户', 1800, '夏季', '["塔尔寺","青海湖","清真"]', 36.6171, 101.7782, 1, NOW(), NOW() FROM provinces p WHERE p.name = '青海省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '银川', 'yinchuan', '塞上江南，西夏古都', 1500, '春秋', '["西夏","影视城","沙湖"]', 38.4872, 106.2309, 1, NOW(), NOW() FROM provinces p WHERE p.name = '宁夏回族自治区';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '海口', 'haikou', '椰城，滨海省会', 2000, '冬季', '["骑楼","火山","海滩"]', 20.0440, 110.1999, 1, NOW(), NOW() FROM provinces p WHERE p.name = '海南省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '珠海', 'zhuhai', '百岛之市，浪漫之城', 2200, '秋冬', '["长隆","海岛","浪漫"]', 22.2710, 113.5767, 1, NOW(), NOW() FROM provinces p WHERE p.name = '广东省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '黄山', 'huangshan', '天下第一奇山', 2800, '春秋', '["黄山","宏村","徽派"]', 29.7147, 118.3375, 1, NOW(), NOW() FROM provinces p WHERE p.name = '安徽省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '敦煌', 'dunhuang', '丝路明珠，飞天故里', 2500, '春秋', '["莫高窟","沙漠","丝路"]', 40.1421, 94.6619, 1, NOW(), NOW() FROM provinces p WHERE p.name = '甘肃省';

INSERT INTO cities (province_id, name, pinyin, description, popularity, best_season, tags, lat, lng, status, created_at, updated_at)
SELECT p.id, '吐鲁番', 'tulufan', '火洲，葡萄之乡', 1500, '春秋', '["火焰山","葡萄","古城"]', 42.9513, 89.1895, 1, NOW(), NOW() FROM provinces p WHERE p.name = '新疆维吾尔自治区';

-- ================= 二、现有景点评分差异化 =================

-- 2.1 先把所有历史硬编码 4.8 的景点按 id 均匀铺开（3.7 ~ 4.8）
UPDATE attractions
SET rating = ROUND(3.7 + (id % 12) * 0.1, 1)
WHERE rating = 4.8;

-- 2.2 对知名景点覆盖高分，保证「体验最优按评分选点」有意义
UPDATE attractions SET rating = CASE name
  WHEN '故宫' THEN 4.9
  WHEN '八达岭长城' THEN 4.9
  WHEN '秦始皇兵马俑' THEN 4.9
  WHEN '西湖' THEN 4.9
  WHEN '布达拉宫' THEN 4.9
  WHEN '九寨沟' THEN 4.9
  WHEN '张家界国家森林公园' THEN 4.9
  WHEN '外滩' THEN 4.8
  WHEN '迪士尼乐园' THEN 4.8
  WHEN '大熊猫繁育研究基地' THEN 4.8
  WHEN '拙政园' THEN 4.8
  WHEN '黄鹤楼' THEN 4.8
  WHEN '龙门石窟' THEN 4.8
  WHEN '漓江' THEN 4.8
  WHEN '乐山大佛' THEN 4.8
  WHEN '玉龙雪山' THEN 4.8
  WHEN '鼓浪屿' THEN 4.8
  WHEN '丽江古城' THEN 4.8
  WHEN '洱海' THEN 4.8
  WHEN '亚龙湾' THEN 4.7
  WHEN '蜈支洲岛' THEN 4.7
  WHEN '都江堰' THEN 4.7
  WHEN '颐和园' THEN 4.8
  WHEN '洪崖洞' THEN 4.7
  WHEN '清明上河园' THEN 4.6
  WHEN '石林' THEN 4.7
  WHEN '崂山' THEN 4.6
  WHEN '千岛湖' THEN 4.7
  WHEN '灵隐寺' THEN 4.6
  WHEN '周庄古镇' THEN 4.7
  WHEN '西安城墙' THEN 4.7
  WHEN '大雁塔' THEN 4.7
  WHEN '东方明珠' THEN 4.7
  WHEN '豫园' THEN 4.6
  WHEN '武侯祠' THEN 4.6
  WHEN '东湖' THEN 4.6
  WHEN '白云山' THEN 4.6
  WHEN '广州塔（小蛮腰）' THEN 4.7
  WHEN '天涯海角' THEN 4.6
  WHEN '岳麓山' THEN 4.6
  WHEN '台北101' THEN 4.6
  WHEN '天山天池' THEN 4.6
  WHEN '华清宫' THEN 4.6
  ELSE rating
END
WHERE name IN (
  '故宫','八达岭长城','秦始皇兵马俑','西湖','布达拉宫','九寨沟','张家界国家森林公园',
  '外滩','迪士尼乐园','大熊猫繁育研究基地','拙政园','黄鹤楼','龙门石窟','漓江','乐山大佛',
  '玉龙雪山','鼓浪屿','丽江古城','洱海','亚龙湾','蜈支洲岛','都江堰','颐和园','洪崖洞',
  '清明上河园','石林','崂山','千岛湖','灵隐寺','周庄古镇','西安城墙','大雁塔','东方明珠',
  '豫园','武侯祠','东湖','白云山','广州塔（小蛮腰）','天涯海角','岳麓山','台北101','天山天池','华清宫'
);

-- ================= 三、新增景点（72 → 220+） =================

INSERT INTO attractions (city_id, name, category, description, rating, ticket_price, suggest_duration, open_time, best_season, status, created_at)
SELECT c.id, t.name, t.category, t.descr, t.rating, t.price, t.dur, t.open_time, t.season, 1, NOW()
FROM (
  -- 哈尔滨
  SELECT '哈尔滨' city, '中央大街' name, '街区' category, '百年老街欧式建筑' descr, 4.7 rating, 0 price, 120 dur, '全天' open_time, '四季' season
  UNION ALL SELECT '哈尔滨', '圣索菲亚大教堂', '宗教', '拜占庭式东正教堂', 4.6, 15, 90, '08:30-17:00', '四季'
  UNION ALL SELECT '哈尔滨', '冰雪大世界', '主题公园', '冰雪主题乐园', 4.8, 330, 240, '09:00-21:00', '冬季'
  -- 长春
  UNION ALL SELECT '长春', '伪满皇宫博物院', '历史遗迹', '末代皇帝溥仪行宫', 4.5, 80, 120, '08:30-16:30', '四季'
  UNION ALL SELECT '长春', '净月潭', '自然景观', '城市森林公园', 4.4, 30, 180, '06:00-18:00', '四季'
  UNION ALL SELECT '长春', '长影世纪城', '主题公园', '电影主题乐园', 4.2, 240, 240, '09:00-17:00', '四季'
  -- 沈阳
  UNION ALL SELECT '沈阳', '沈阳故宫', '历史遗迹', '清初皇宫', 4.6, 60, 120, '08:30-17:00', '四季'
  UNION ALL SELECT '沈阳', '张氏帅府', '历史遗迹', '张作霖张学良故居', 4.5, 50, 90, '08:30-17:00', '四季'
  UNION ALL SELECT '沈阳', '北陵公园', '历史遗迹', '清昭陵', 4.4, 30, 120, '07:00-18:00', '四季'
  -- 大连
  UNION ALL SELECT '大连', '星海广场', '街区', '亚洲最大城市广场', 4.6, 0, 90, '全天', '夏季'
  UNION ALL SELECT '大连', '老虎滩海洋公园', '主题公园', '海洋动物乐园', 4.5, 220, 240, '08:30-17:00', '夏季'
  UNION ALL SELECT '大连', '金石滩', '自然景观', '海滨地质公园', 4.6, 60, 240, '08:00-17:00', '夏季'
  -- 郑州
  UNION ALL SELECT '郑州', '少林寺', '宗教', '天下第一名刹', 4.8, 100, 240, '07:30-18:00', '四季'
  UNION ALL SELECT '郑州', '河南博物院', '博物馆', '中原文物重镇', 4.6, 0, 180, '09:00-17:00', '四季'
  UNION ALL SELECT '郑州', '黄河风景名胜区', '自然景观', '黄河中下游分界', 4.3, 60, 180, '08:00-18:00', '春秋'
  -- 济南
  UNION ALL SELECT '济南', '趵突泉', '自然景观', '天下第一泉', 4.7, 40, 120, '07:00-18:00', '春秋'
  UNION ALL SELECT '济南', '大明湖', '自然景观', '泉城明珠', 4.6, 30, 180, '06:00-18:00', '春秋'
  UNION ALL SELECT '济南', '千佛山', '自然景观', '佛教名山', 4.4, 30, 180, '07:00-18:00', '春秋'
  -- 石家庄
  UNION ALL SELECT '石家庄', '正定古城', '历史遗迹', '千年古县城', 4.4, 0, 180, '全天', '春秋'
  UNION ALL SELECT '石家庄', '西柏坡', '历史遗迹', '革命圣地', 4.5, 0, 120, '08:00-17:00', '春秋'
  UNION ALL SELECT '石家庄', '苍岩山', '自然景观', '太行名山', 4.2, 50, 240, '08:00-17:00', '春秋'
  -- 太原
  UNION ALL SELECT '太原', '晋祠', '历史遗迹', '晋文化源头', 4.7, 80, 180, '08:00-18:00', '春秋'
  UNION ALL SELECT '太原', '山西博物院', '博物馆', '三晋文物', 4.6, 0, 180, '09:00-17:00', '四季'
  UNION ALL SELECT '太原', '蒙山大佛', '历史遗迹', '北齐摩崖大佛', 4.3, 50, 120, '08:00-17:00', '春秋'
  -- 呼和浩特
  UNION ALL SELECT '呼和浩特', '大召寺', '宗教', '藏传佛教名寺', 4.4, 35, 90, '08:00-17:00', '夏季'
  UNION ALL SELECT '呼和浩特', '内蒙古博物院', '博物馆', '草原文明', 4.5, 0, 180, '09:00-17:00', '四季'
  UNION ALL SELECT '呼和浩特', '希拉穆仁草原', '自然景观', '呼和浩特草原', 4.6, 100, 240, '全天', '夏季'
  -- 福州
  UNION ALL SELECT '福州', '三坊七巷', '街区', '明清古建筑群', 4.7, 0, 180, '全天', '秋冬'
  UNION ALL SELECT '福州', '鼓山', '自然景观', '福州名山', 4.4, 40, 180, '06:00-18:00', '四季'
  UNION ALL SELECT '福州', '林则徐纪念馆', '历史遗迹', '民族英雄故居', 4.5, 0, 90, '08:30-17:00', '四季'
  -- 南昌
  UNION ALL SELECT '南昌', '滕王阁', '历史遗迹', '江南三大名楼之一', 4.7, 50, 120, '08:00-18:00', '春秋'
  UNION ALL SELECT '南昌', '八一广场', '街区', '南昌地标广场', 4.4, 0, 60, '全天', '四季'
  UNION ALL SELECT '南昌', '绳金塔', '历史遗迹', '南昌古塔', 4.3, 20, 90, '08:00-17:00', '四季'
  -- 合肥
  UNION ALL SELECT '合肥', '三河古镇', '街区', '水乡古镇', 4.4, 0, 180, '全天', '春秋'
  UNION ALL SELECT '合肥', '包公园', '历史遗迹', '包拯纪念园', 4.5, 40, 120, '08:00-17:30', '春秋'
  UNION ALL SELECT '合肥', '安徽省博物馆', '博物馆', '徽派文物', 4.5, 0, 180, '09:00-17:00', '四季'
  -- 南宁
  UNION ALL SELECT '南宁', '青秀山', '自然景观', '绿城名片', 4.5, 20, 240, '06:00-18:00', '四季'
  UNION ALL SELECT '南宁', '南湖公园', '自然景观', '城市湖泊', 4.3, 0, 120, '全天', '四季'
  UNION ALL SELECT '南宁', '广西民族博物馆', '博物馆', '壮乡文化', 4.4, 0, 180, '09:00-17:00', '四季'
  -- 贵阳
  UNION ALL SELECT '贵阳', '甲秀楼', '历史遗迹', '贵阳地标', 4.4, 10, 60, '08:30-17:00', '四季'
  UNION ALL SELECT '贵阳', '黔灵山公园', '自然景观', '城市山岳公园', 4.5, 5, 180, '06:30-18:00', '夏季'
  UNION ALL SELECT '贵阳', '青岩古镇', '街区', '明清古镇', 4.6, 60, 180, '08:00-17:30', '春秋'
  -- 兰州
  UNION ALL SELECT '兰州', '黄河铁桥', '历史遗迹', '天下黄河第一桥', 4.5, 0, 60, '全天', '春秋'
  UNION ALL SELECT '兰州', '甘肃省博物馆', '博物馆', '丝路文物', 4.6, 0, 180, '09:00-17:00', '四季'
  UNION ALL SELECT '兰州', '五泉山', '自然景观', '兰州名山', 4.3, 20, 180, '06:00-18:00', '春秋'
  -- 西宁
  UNION ALL SELECT '西宁', '塔尔寺', '宗教', '藏传佛教六大寺之一', 4.7, 80, 180, '08:00-17:00', '夏季'
  UNION ALL SELECT '西宁', '青海湖', '自然景观', '中国最大内陆湖', 4.8, 100, 240, '全天', '夏季'
  UNION ALL SELECT '西宁', '东关清真大寺', '宗教', '西北最大清真寺', 4.4, 0, 60, '08:00-18:00', '四季'
  -- 银川
  UNION ALL SELECT '银川', '镇北堡西部影城', '主题公园', '影视拍摄基地', 4.6, 100, 240, '08:00-18:00', '春秋'
  UNION ALL SELECT '银川', '西夏王陵', '历史遗迹', '西夏王朝陵墓', 4.5, 75, 120, '08:00-17:30', '春秋'
  UNION ALL SELECT '银川', '沙湖', '自然景观', '沙漠湖泊', 4.5, 60, 240, '08:00-17:00', '夏季'
  -- 海口
  UNION ALL SELECT '海口', '骑楼老街', '街区', '南洋风情骑楼', 4.4, 0, 120, '全天', '冬季'
  UNION ALL SELECT '海口', '火山口公园', '自然景观', '火山地质公园', 4.5, 60, 180, '08:00-18:00', '冬季'
  UNION ALL SELECT '海口', '假日海滩', '自然景观', '滨海休闲海滩', 4.3, 0, 120, '全天', '冬季'
  -- 珠海
  UNION ALL SELECT '珠海', '长隆海洋王国', '主题公园', '海洋主题乐园', 4.8, 395, 240, '10:00-20:00', '秋冬'
  UNION ALL SELECT '珠海', '情侣路', '街区', '滨海景观大道', 4.5, 0, 90, '全天', '秋冬'
  UNION ALL SELECT '珠海', '圆明新园', '主题公园', '仿圆明园园林', 4.2, 100, 180, '08:30-17:30', '四季'
  -- 黄山
  UNION ALL SELECT '黄山', '黄山风景区', '自然景观', '天下第一奇山', 4.9, 190, 240, '06:30-16:30', '春秋'
  UNION ALL SELECT '黄山', '宏村', '街区', '画里乡村', 4.8, 104, 180, '08:00-17:30', '春秋'
  UNION ALL SELECT '黄山', '西递', '街区', '徽派古村落', 4.7, 104, 180, '08:00-17:30', '春秋'
  -- 敦煌
  UNION ALL SELECT '敦煌', '莫高窟', '历史遗迹', '世界文化遗产石窟', 4.9, 238, 240, '08:00-18:00', '春秋'
  UNION ALL SELECT '敦煌', '鸣沙山月牙泉', '自然景观', '沙漠奇观', 4.8, 110, 180, '07:00-19:00', '春秋'
  UNION ALL SELECT '敦煌', '玉门关', '历史遗迹', '丝路关隘遗址', 4.4, 40, 120, '08:00-18:00', '春秋'
  -- 吐鲁番
  UNION ALL SELECT '吐鲁番', '火焰山', '自然景观', '西游记火焰山', 4.4, 40, 120, '08:00-19:00', '春秋'
  UNION ALL SELECT '吐鲁番', '葡萄沟', '自然景观', '葡萄之乡', 4.5, 60, 180, '08:00-19:00', '夏'
  UNION ALL SELECT '吐鲁番', '交河故城', '历史遗迹', '古代城址', 4.5, 70, 180, '08:00-18:00', '春秋'
  -- 北京（补充）
  UNION ALL SELECT '北京', '香山公园', '自然景观', '红叶胜地', 4.6, 10, 180, '06:00-18:00', '秋'
  UNION ALL SELECT '北京', '什刹海', '街区', '老北京风情', 4.6, 0, 120, '全天', '四季'
  UNION ALL SELECT '北京', '雍和宫', '宗教', '藏传佛教名寺', 4.7, 25, 120, '09:00-16:00', '四季'
  -- 上海（补充）
  UNION ALL SELECT '上海', '上海野生动物园', '主题公园', '动物王国', 4.6, 165, 240, '09:00-16:00', '四季'
  UNION ALL SELECT '上海', '田子坊', '街区', '文艺弄堂', 4.5, 0, 120, '全天', '四季'
  UNION ALL SELECT '上海', '朱家角古镇', '街区', '江南水乡', 4.5, 0, 180, '全天', '春秋'
  -- 重庆（补充）
  UNION ALL SELECT '重庆', '武隆天生三桥', '自然景观', '喀斯特天桥', 4.8, 95, 240, '08:30-16:30', '春秋'
  UNION ALL SELECT '重庆', '大足石刻', '历史遗迹', '世界文化遗产石刻', 4.7, 115, 180, '08:30-17:30', '春秋'
  UNION ALL SELECT '重庆', '南山一棵树', '自然景观', '重庆夜景观景台', 4.4, 30, 60, '09:00-22:00', '四季'
  -- 成都（补充）
  UNION ALL SELECT '成都', '青城山', '自然景观', '道教名山', 4.7, 90, 240, '08:00-17:00', '春秋'
  UNION ALL SELECT '成都', '杜甫草堂', '历史遗迹', '杜甫故居', 4.6, 50, 120, '08:00-18:00', '四季'
  UNION ALL SELECT '成都', '西岭雪山', '自然景观', '雪山滑雪场', 4.5, 120, 240, '08:00-17:00', '冬'
  -- 西安（补充）
  UNION ALL SELECT '西安', '大唐不夜城', '街区', '唐风步行街', 4.7, 0, 180, '18:00-23:00', '四季'
  UNION ALL SELECT '西安', '回民街', '街区', '美食一条街', 4.6, 0, 120, '全天', '四季'
  UNION ALL SELECT '西安', '华山', '自然景观', '五岳之一', 4.8, 160, 240, '全天', '春秋'
  -- 杭州（补充）
  UNION ALL SELECT '杭州', '西溪湿地', '自然景观', '城市湿地', 4.6, 80, 240, '08:00-17:30', '春秋'
  UNION ALL SELECT '杭州', '雷峰塔', '历史遗迹', '白蛇传传说', 4.5, 40, 90, '08:00-17:00', '四季'
  UNION ALL SELECT '杭州', '宋城', '主题公园', '宋代文化乐园', 4.4, 300, 240, '09:00-21:00', '四季'
  -- 南京（补充）
  UNION ALL SELECT '南京', '总统府', '历史遗迹', '民国建筑群', 4.6, 40, 120, '08:30-17:00', '春秋'
  UNION ALL SELECT '南京', '鸡鸣寺', '宗教', '千年古寺', 4.5, 15, 90, '07:30-17:00', '春秋'
  UNION ALL SELECT '南京', '牛首山', '自然景观', '佛教文化山', 4.5, 98, 180, '08:30-17:00', '春秋'
  -- 苏州（补充）
  UNION ALL SELECT '苏州', '狮子林', '历史遗迹', '假山园林', 4.6, 40, 120, '07:30-17:30', '四季'
  UNION ALL SELECT '苏州', '留园', '历史遗迹', '四大名园之一', 4.6, 55, 120, '07:30-17:00', '四季'
  UNION ALL SELECT '苏州', '寒山寺', '宗教', '姑苏城外古寺', 4.5, 20, 90, '08:00-17:00', '四季'
  -- 武汉（补充）
  UNION ALL SELECT '武汉', '湖北省博物馆', '博物馆', '越王勾践剑', 4.6, 0, 180, '09:00-17:00', '四季'
  UNION ALL SELECT '武汉', '户部巷', '街区', '武汉小吃街', 4.5, 0, 90, '全天', '四季'
  UNION ALL SELECT '武汉', '木兰天池', '自然景观', '木兰故里山水', 4.3, 80, 240, '08:00-17:00', '春秋'
  -- 广州（补充）
  UNION ALL SELECT '广州', '长隆野生动物世界', '主题公园', '亚洲最大动物园', 4.8, 300, 240, '09:30-18:00', '四季'
  UNION ALL SELECT '广州', '沙面', '街区', '欧式建筑岛', 4.5, 0, 120, '全天', '四季'
  UNION ALL SELECT '广州', '越秀公园', '自然景观', '羊城地标公园', 4.4, 0, 120, '06:00-22:00', '四季'
  -- 深圳（补充）
  UNION ALL SELECT '深圳', '锦绣中华', '主题公园', '微缩中国名胜', 4.5, 220, 180, '09:00-21:30', '四季'
  UNION ALL SELECT '深圳', '大梅沙', '自然景观', '免费海滨浴场', 4.4, 0, 180, '全天', '夏季'
  UNION ALL SELECT '深圳', '莲花山公园', '自然景观', '城市公园', 4.4, 0, 90, '06:00-22:00', '四季'
  -- 昆明（补充）
  UNION ALL SELECT '昆明', '九乡', '自然景观', '溶洞奇观', 4.4, 90, 240, '08:30-17:00', '四季'
  UNION ALL SELECT '昆明', '云南民族村', '主题公园', '民族文化村', 4.4, 90, 180, '08:30-17:30', '四季'
  UNION ALL SELECT '昆明', '西山龙门', '自然景观', '滇池西山', 4.5, 40, 180, '08:00-17:00', '四季'
  -- 大理（补充）
  UNION ALL SELECT '大理', '双廊', '街区', '洱海渔村', 4.6, 0, 120, '全天', '四季'
  UNION ALL SELECT '大理', '喜洲古镇', '街区', '白族古镇', 4.5, 0, 120, '全天', '四季'
  UNION ALL SELECT '大理', '天龙八部影视城', '主题公园', '影视基地', 4.2, 60, 120, '08:00-18:00', '四季'
  -- 丽江（补充）
  UNION ALL SELECT '丽江', '泸沽湖', '自然景观', '高原湖泊', 4.8, 100, 240, '全天', '春秋'
  UNION ALL SELECT '丽江', '拉市海', '自然景观', '高原湿地', 4.3, 30, 120, '08:00-17:00', '四季'
  UNION ALL SELECT '丽江', '黑龙潭', '自然景观', '丽江古潭', 4.3, 50, 90, '07:00-19:00', '四季'
  -- 三亚（补充）
  UNION ALL SELECT '三亚', '南山文化旅游区', '主题公园', '佛教文化园', 4.7, 145, 240, '08:00-17:00', '冬季'
  UNION ALL SELECT '三亚', '大小洞天', '自然景观', '海滨奇观', 4.4, 130, 180, '08:00-17:30', '冬季'
  UNION ALL SELECT '三亚', '三亚千古情', '主题公园', '演艺乐园', 4.5, 300, 180, '10:00-21:00', '冬季'
  -- 青岛（补充）
  UNION ALL SELECT '青岛', '青岛啤酒博物馆', '博物馆', '啤酒文化', 4.6, 60, 120, '08:30-17:00', '春夏秋'
  UNION ALL SELECT '青岛', '奥帆中心', '街区', '奥运帆船基地', 4.4, 0, 90, '全天', '夏季'
  UNION ALL SELECT '青岛', '信号山', '自然景观', '俯瞰红瓦绿树', 4.4, 15, 90, '07:30-18:00', '春夏秋'
  -- 厦门（补充）
  UNION ALL SELECT '厦门', '曾厝垵', '街区', '文艺渔村', 4.5, 0, 120, '全天', '秋冬'
  UNION ALL SELECT '厦门', '环岛路', '街区', '滨海骑行道', 4.6, 0, 120, '全天', '秋冬'
  UNION ALL SELECT '厦门', '万石植物园', '自然景观', '植物王国', 4.5, 40, 180, '06:30-18:00', '四季'
  -- 长沙（补充）
  UNION ALL SELECT '长沙', '湖南省博物馆', '博物馆', '马王堆汉墓', 4.6, 0, 180, '09:00-17:00', '四季'
  UNION ALL SELECT '长沙', '太平街', '街区', '长沙老街', 4.5, 0, 120, '全天', '四季'
  UNION ALL SELECT '长沙', '长沙世界之窗', '主题公园', '主题乐园', 4.3, 130, 240, '09:00-18:00', '四季'
  -- 张家界（补充）
  UNION ALL SELECT '张家界', '天门山', '自然景观', '玻璃栈道', 4.8, 275, 240, '08:00-16:00', '春秋'
  UNION ALL SELECT '张家界', '黄龙洞', '自然景观', '溶洞奇观', 4.5, 100, 180, '08:00-17:00', '春秋'
  UNION ALL SELECT '张家界', '宝峰湖', '自然景观', '高山湖泊', 4.4, 96, 180, '08:00-17:00', '春秋'
  -- 洛阳（补充）
  UNION ALL SELECT '洛阳', '洛阳博物馆', '博物馆', '河洛文明', 4.5, 0, 180, '09:00-17:00', '四季'
  UNION ALL SELECT '洛阳', '关林', '历史遗迹', '关羽葬首之地', 4.4, 40, 120, '08:00-17:00', '春秋'
  UNION ALL SELECT '洛阳', '白云山', '自然景观', '中原名山', 4.4, 75, 240, '08:00-17:00', '夏'
  -- 开封（补充）
  UNION ALL SELECT '开封', '龙亭公园', '历史遗迹', '宋代皇宫遗址', 4.3, 35, 120, '08:00-17:30', '春秋'
  UNION ALL SELECT '开封', '大相国寺', '宗教', '皇家寺院', 4.4, 40, 90, '08:00-17:00', '春秋'
  UNION ALL SELECT '开封', '包公祠', '历史遗迹', '包拯纪念祠', 4.3, 30, 90, '08:00-17:30', '春秋'
  -- 九寨沟（补充）
  UNION ALL SELECT '九寨沟', '黄龙', '自然景观', '钙华彩池', 4.8, 170, 240, '08:00-17:00', '9-10月'
  UNION ALL SELECT '九寨沟', '神仙池', '自然景观', '森林湖泊', 4.3, 100, 180, '08:00-17:00', '9-10月'
  UNION ALL SELECT '九寨沟', '牟尼沟', '自然景观', '瀑布彩池', 4.3, 70, 180, '08:00-17:00', '9-10月'
  -- 乐山（补充）
  UNION ALL SELECT '乐山', '峨眉山', '自然景观', '四大佛教名山', 4.8, 160, 240, '06:00-18:00', '春秋'
  UNION ALL SELECT '乐山', '乌尤寺', '宗教', '古寺', 4.2, 10, 60, '08:00-17:00', '四季'
  UNION ALL SELECT '乐山', '东方佛都', '宗教', '佛像群', 4.2, 80, 120, '08:00-17:30', '四季'
  -- 桂林（补充）
  UNION ALL SELECT '桂林', '龙脊梯田', '自然景观', '世界梯田奇观', 4.7, 80, 240, '全天', '4-10月'
  UNION ALL SELECT '桂林', '银子岩', '自然景观', '溶洞奇观', 4.5, 80, 120, '08:00-17:30', '四季'
  UNION ALL SELECT '桂林', '阳朔西街', '街区', '阳朔老街', 4.5, 0, 120, '全天', '4-10月'
  -- 拉萨（补充）
  UNION ALL SELECT '拉萨', '大昭寺', '宗教', '藏传佛教圣寺', 4.8, 85, 180, '09:00-17:00', '5-10月'
  UNION ALL SELECT '拉萨', '八廓街', '街区', '拉萨老城', 4.6, 0, 120, '全天', '5-10月'
  UNION ALL SELECT '拉萨', '纳木错', '自然景观', '圣湖', 4.8, 120, 240, '全天', '6-9月'
  -- 乌鲁木齐（补充）
  UNION ALL SELECT '乌鲁木齐', '大巴扎', '街区', '国际大巴扎', 4.5, 0, 120, '全天', '6-9月'
  UNION ALL SELECT '乌鲁木齐', '红山公园', '自然景观', '城市地标', 4.3, 10, 90, '08:00-21:00', '四季'
  UNION ALL SELECT '乌鲁木齐', '天山大峡谷', '自然景观', '峡谷风光', 4.6, 100, 240, '08:00-17:00', '6-9月'
  -- 台北（补充）
  UNION ALL SELECT '台北', '台北故宫博物院', '博物馆', '中华文物', 4.8, 100, 240, '09:00-17:00', '秋冬'
  UNION ALL SELECT '台北', '阳明山', '自然景观', '火山公园', 4.4, 0, 180, '全天', '秋冬'
  UNION ALL SELECT '台北', '士林夜市', '街区', '台北夜市', 4.6, 0, 120, '17:00-24:00', '秋冬'
  -- 天津（补充）
  UNION ALL SELECT '天津', '五大道', '街区', '万国建筑群', 4.6, 0, 120, '全天', '春秋'
  UNION ALL SELECT '天津', '意式风情区', '街区', '意大利风情街', 4.4, 0, 90, '全天', '春秋'
  UNION ALL SELECT '天津', '天津之眼', '主题公园', '摩天轮', 4.5, 70, 60, '09:30-21:30', '春秋'
) t
JOIN cities c ON c.name = t.city;

-- ================= 四、酒店数据 =================

INSERT INTO hotels (city_id, name, star_level, price_per_night, rating, address, tags, status, created_at, updated_at)
SELECT c.id, t.name, t.star, t.price, t.rating, t.address, t.tags, 1, NOW(), NOW()
FROM (
  SELECT '北京' city, '北京饭店' name, 5 star, 1200 price, 4.8 rating, '东城区长安街' address, '["豪华","历史"]' tags
  UNION ALL SELECT '北京', '如家快捷·王府井店', 3, 320, 4.4, '东城区王府井', '["经济","连锁","地铁"]'
  UNION ALL SELECT '上海', '和平饭店', 5, 1800, 4.9, '黄浦区南京东路', '["豪华","江景","历史"]'
  UNION ALL SELECT '上海', '汉庭酒店·外滩店', 3, 350, 4.4, '黄浦区中山东一路', '["经济","连锁","江景"]'
  UNION ALL SELECT '成都', '成都博舍', 5, 1500, 4.8, '锦江区笔帖式街', '["豪华","设计","市中心"]'
  UNION ALL SELECT '成都', '7天连锁·春熙路店', 2, 200, 4.2, '锦江区春熙路', '["经济","连锁"]'
  UNION ALL SELECT '重庆', '重庆威斯汀酒店', 5, 1100, 4.7, '渝中区新华路', '["豪华","江景","解放碑"]'
  UNION ALL SELECT '重庆', '锦江之星·观音桥店', 3, 260, 4.3, '江北区观音桥', '["经济","连锁"]'
  UNION ALL SELECT '西安', '西安索菲特传奇', 5, 1300, 4.8, '新城区东新街', '["豪华","历史","城墙"]'
  UNION ALL SELECT '西安', '全季酒店·钟楼店', 4, 380, 4.5, '碑林区钟楼', '["舒适","连锁"]'
  UNION ALL SELECT '杭州', '杭州西子湖四季酒店', 5, 2200, 4.9, '西湖区灵隐路', '["豪华","西湖","度假"]'
  UNION ALL SELECT '杭州', '亚朵酒店·西湖店', 4, 450, 4.6, '上城区南山路', '["舒适","设计","西湖"]'
  UNION ALL SELECT '南京', '金陵饭店', 5, 900, 4.6, '鼓楼区汉中路', '["豪华","历史","市中心"]'
  UNION ALL SELECT '南京', '莫泰168·新街口店', 3, 240, 4.2, '秦淮区新街口', '["经济","连锁"]'
  UNION ALL SELECT '苏州', '苏州金鸡湖大酒店', 5, 1000, 4.7, '工业园区金鸡湖', '["豪华","湖景","度假"]'
  UNION ALL SELECT '苏州', '桔子酒店·平江路店', 4, 400, 4.5, '姑苏区平江路', '["舒适","古镇"]'
  UNION ALL SELECT '武汉', '武汉万达瑞华酒店', 5, 980, 4.7, '武昌区水果湖', '["豪华","东湖","湖景"]'
  UNION ALL SELECT '武汉', '宜必思·江汉路店', 3, 280, 4.3, '江汉区江汉路', '["经济","连锁"]'
  UNION ALL SELECT '广州', '广州四季酒店', 5, 1600, 4.9, '天河区珠江新城', '["豪华","江景","地标"]'
  UNION ALL SELECT '广州', '城市便捷·北京路店', 2, 220, 4.1, '越秀区北京路', '["经济","连锁"]'
  UNION ALL SELECT '深圳', '深圳君悦酒店', 5, 1200, 4.7, '罗湖区宝安南路', '["豪华","地标","购物"]'
  UNION ALL SELECT '深圳', '维也纳·华强北店', 3, 300, 4.3, '福田区华强北', '["经济","连锁"]'
  UNION ALL SELECT '三亚', '三亚亚特兰蒂斯', 5, 2800, 4.9, '海棠区海棠湾', '["豪华","度假","亲子"]'
  UNION ALL SELECT '三亚', '三亚海景民宿·大东海店', 3, 350, 4.4, '吉阳区大东海', '["海景","民宿"]'
  UNION ALL SELECT '青岛', '青岛海尔洲际酒店', 5, 1100, 4.6, '市南区奥帆中心', '["豪华","海景"]'
  UNION ALL SELECT '青岛', '汉庭·栈桥店', 3, 260, 4.2, '市南区中山路', '["经济","连锁","海边"]'
  UNION ALL SELECT '厦门', '厦门康莱德酒店', 5, 1300, 4.8, '思明区演武大桥', '["豪华","海景","鼓浪屿"]'
  UNION ALL SELECT '厦门', '如家·中山路店', 3, 280, 4.3, '思明区中山路', '["经济","连锁"]'
  UNION ALL SELECT '长沙', '长沙尼依格罗酒店', 5, 1000, 4.7, '芙蓉区解放西路', '["豪华","江景","地标"]'
  UNION ALL SELECT '长沙', '7天·五一广场店', 2, 190, 4.1, '天心区五一广场', '["经济","连锁"]'
  UNION ALL SELECT '昆明', '昆明洲际酒店', 5, 900, 4.6, '西山区滇池路', '["豪华","滇池","度假"]'
  UNION ALL SELECT '昆明', '如家·翠湖店', 3, 240, 4.2, '五华区翠湖', '["经济","连锁"]'
  UNION ALL SELECT '大理', '大理海纳尔云墅', 5, 1500, 4.8, '大理市洱海边', '["豪华","洱海","度假"]'
  UNION ALL SELECT '大理', '大理古城客栈', 3, 300, 4.5, '大理市古城', '["民宿","古城"]'
  UNION ALL SELECT '丽江', '丽江悦榕庄', 5, 1800, 4.8, '古城区束河', '["豪华","雪山","度假"]'
  UNION ALL SELECT '丽江', '丽江古城客栈', 3, 280, 4.5, '古城区大研', '["民宿","古城"]'
  UNION ALL SELECT '桂林', '桂林香格里拉', 5, 800, 4.6, '七星区漓江边', '["豪华","漓江"]'
  UNION ALL SELECT '桂林', '如家·两江四湖店', 3, 230, 4.2, '象山区象鼻山', '["经济","连锁"]'
  UNION ALL SELECT '拉萨', '拉萨圣地天堂洲际', 5, 950, 4.6, '城关区北京西路', '["豪华","高原","藏式"]'
  UNION ALL SELECT '拉萨', '拉萨平措康桑客栈', 3, 280, 4.5, '城关区八廓街', '["民宿","藏式"]'
  UNION ALL SELECT '天津', '天津丽思卡尔顿', 5, 1050, 4.7, '和平区大沽北路', '["豪华","历史","海河"]'
  UNION ALL SELECT '天津', '如家·小白楼店', 3, 250, 4.2, '和平区小白楼', '["经济","连锁"]'
  UNION ALL SELECT '洛阳', '洛阳华阳广场国际大酒店', 4, 500, 4.4, '西工区凯旋路', '["舒适","市中心"]'
  UNION ALL SELECT '洛阳', '汉庭·龙门大道店', 3, 230, 4.2, '洛龙区龙门大道', '["经济","连锁"]'
  UNION ALL SELECT '开封', '开封开元名都大酒店', 5, 620, 4.5, '龙亭区大梁路', '["豪华","湖畔"]'
  UNION ALL SELECT '开封', '如家·鼓楼店', 3, 210, 4.1, '鼓楼区鼓楼街', '["经济","连锁"]'
  UNION ALL SELECT '张家界', '张家界京武铂尔曼', 5, 880, 4.7, '武陵源区画卷路', '["豪华","山景","度假"]'
  UNION ALL SELECT '张家界', '如家·天门山店', 3, 240, 4.2, '永定区大庸路', '["经济","连锁"]'
  UNION ALL SELECT '九寨沟', '九寨沟喜来登', 5, 1100, 4.7, '漳扎镇', '["豪华","景区","度假"]'
  UNION ALL SELECT '九寨沟', '九寨沟沟口客栈', 3, 320, 4.4, '漳扎镇沟口', '["民宿","景区"]'
  UNION ALL SELECT '乐山', '乐山金海棠大酒店', 4, 420, 4.3, '市中区海棠路', '["舒适","市中心"]'
  UNION ALL SELECT '乐山', '如家·大佛景区店', 3, 220, 4.1, '市中区滨江路', '["经济","连锁"]'
  UNION ALL SELECT '乌鲁木齐', '乌鲁木齐环球铂尔曼', 5, 780, 4.6, '天山区解放北路', '["豪华","市中心"]'
  UNION ALL SELECT '乌鲁木齐', '7天·大巴扎店', 2, 180, 4.1, '天山区解放南路', '["经济","连锁"]'
  UNION ALL SELECT '台北', '台北W饭店', 5, 1500, 4.8, '信义区忠孝东路', '["豪华","地标","101"]'
  UNION ALL SELECT '台北', '台北青年旅舍·西门町', 3, 300, 4.3, '万华区西门町', '["青旅","西门町"]'
  UNION ALL SELECT '哈尔滨', '哈尔滨香格里拉', 5, 700, 4.6, '道里区友谊路', '["豪华","江景","冰雪"]'
  UNION ALL SELECT '哈尔滨', '如家·中央大街店', 3, 220, 4.3, '道里区中央大街', '["经济","连锁"]'
  UNION ALL SELECT '长春', '长春香格里拉', 5, 650, 4.5, '朝阳区西安大路', '["豪华","市中心"]'
  UNION ALL SELECT '长春', '汉庭·净月潭店', 3, 210, 4.2, '南关区净月大街', '["经济","连锁"]'
  UNION ALL SELECT '沈阳', '沈阳君悦酒店', 5, 720, 4.6, '和平区青年大街', '["豪华","市中心"]'
  UNION ALL SELECT '沈阳', '如家·故宫店', 3, 230, 4.2, '沈河区沈阳路', '["经济","连锁"]'
  UNION ALL SELECT '大连', '大连一方城堡豪华精选', 5, 1000, 4.7, '中山区星海湾', '["豪华","海景","城堡"]'
  UNION ALL SELECT '大连', '汉庭·青泥洼桥店', 3, 250, 4.2, '中山区青泥洼桥', '["经济","连锁"]'
  UNION ALL SELECT '郑州', '郑州裕达国贸酒店', 5, 580, 4.4, '中原区中原中路', '["豪华","市中心"]'
  UNION ALL SELECT '郑州', '如家·二七广场店', 3, 220, 4.1, '二七区二七广场', '["经济","连锁"]'
  UNION ALL SELECT '济南', '济南贵和皇冠假日', 5, 680, 4.6, '历下区泉城路', '["豪华","泉城"]'
  UNION ALL SELECT '济南', '汉庭·大明湖店', 3, 240, 4.2, '历下区大明湖路', '["经济","连锁"]'
  UNION ALL SELECT '石家庄', '石家庄希尔顿', 5, 600, 4.5, '长安区东大街', '["豪华","市中心"]'
  UNION ALL SELECT '石家庄', '如家·北国商城店', 3, 200, 4.1, '长安区中山东路', '["经济","连锁"]'
  UNION ALL SELECT '太原', '太原凯宾斯基', 5, 660, 4.5, '小店区长风街', '["豪华","市中心"]'
  UNION ALL SELECT '太原', '汉庭·柳巷店', 3, 210, 4.2, '迎泽区柳巷', '["经济","连锁"]'
  UNION ALL SELECT '呼和浩特', '呼和浩特香格里拉', 5, 620, 4.5, '新城区锡林郭勒南路', '["豪华","市中心"]'
  UNION ALL SELECT '呼和浩特', '如家·大召寺店', 3, 190, 4.1, '玉泉区大南街', '["经济","连锁"]'
  UNION ALL SELECT '福州', '福州世茂洲际', 5, 640, 4.5, '台江区江滨中大道', '["豪华","江景"]'
  UNION ALL SELECT '福州', '汉庭·三坊七巷店', 3, 230, 4.3, '鼓楼区杨桥东路', '["经济","连锁"]'
  UNION ALL SELECT '南昌', '南昌喜来登', 5, 600, 4.5, '红谷滩区赣江中大道', '["豪华","江景"]'
  UNION ALL SELECT '南昌', '如家·滕王阁店', 3, 220, 4.2, '东湖区叠山路', '["经济","连锁"]'
  UNION ALL SELECT '合肥', '合肥威斯汀', 5, 620, 4.5, '包河区徽州大道', '["豪华","市中心"]'
  UNION ALL SELECT '合肥', '汉庭·三孝口店', 3, 210, 4.2, '庐阳区长江中路', '["经济","连锁"]'
  UNION ALL SELECT '南宁', '南宁万达文华', 5, 560, 4.5, '青秀区民族大道', '["豪华","市中心"]'
  UNION ALL SELECT '南宁', '如家·朝阳广场店', 3, 200, 4.1, '兴宁区朝阳路', '["经济","连锁"]'
  UNION ALL SELECT '贵阳', '贵阳凯宾斯基', 5, 580, 4.5, '南明区护国路', '["豪华","市中心"]'
  UNION ALL SELECT '贵阳', '汉庭·甲秀楼店', 3, 200, 4.2, '南明区新华路', '["经济","连锁"]'
  UNION ALL SELECT '兰州', '兰州皇冠假日', 5, 520, 4.4, '城关区北滨河东路', '["豪华","黄河"]'
  UNION ALL SELECT '兰州', '如家·张掖路店', 3, 190, 4.1, '城关区张掖路', '["经济","连锁"]'
  UNION ALL SELECT '西宁', '西宁索菲特', 5, 560, 4.5, '城中区西大街', '["豪华","市中心"]'
  UNION ALL SELECT '西宁', '汉庭·大十字店', 3, 200, 4.2, '城中区大十字', '["经济","连锁"]'
  UNION ALL SELECT '银川', '银川喜来登', 5, 520, 4.4, '金凤区上海西路', '["豪华","市中心"]'
  UNION ALL SELECT '银川', '如家·西夏区店', 3, 190, 4.1, '西夏区怀远东路', '["经济","连锁"]'
  UNION ALL SELECT '海口', '海口万丽', 5, 700, 4.6, '龙华区滨海大道', '["豪华","滨海"]'
  UNION ALL SELECT '海口', '汉庭·骑楼老街店', 3, 220, 4.2, '龙华区中山路', '["经济","连锁"]'
  UNION ALL SELECT '珠海', '珠海长隆横琴湾酒店', 5, 1100, 4.8, '横琴新区长隆大道', '["豪华","长隆","亲子"]'
  UNION ALL SELECT '珠海', '如家·拱北口岸店', 3, 240, 4.2, '香洲区拱北口岸', '["经济","连锁"]'
  UNION ALL SELECT '黄山', '黄山悦榕庄', 5, 1500, 4.8, '黄山区汤口镇', '["豪华","黄山","度假"]'
  UNION ALL SELECT '黄山', '黄山山下客栈', 3, 300, 4.3, '黄山区汤口镇', '["民宿","山景"]'
  UNION ALL SELECT '敦煌', '敦煌山庄', 5, 800, 4.7, '敦煌市鸣沙山路', '["豪华","沙漠","度假"]'
  UNION ALL SELECT '敦煌', '敦煌青年旅舍', 3, 180, 4.2, '敦煌市沙州北路', '["青旅","经济"]'
  UNION ALL SELECT '吐鲁番', '吐鲁番火洲大酒店', 4, 350, 4.2, '高昌区绿洲中路', '["舒适","市中心"]'
  UNION ALL SELECT '吐鲁番', '吐鲁番葡萄庄园客栈', 3, 200, 4.3, '高昌区葡萄沟', '["民宿","葡萄"]'
) t
JOIN cities c ON c.name = t.city;

