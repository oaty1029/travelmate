# -*- coding: utf-8 -*-
"""景点名 → 英文搜索词（Commons 英文检索质量更高），未收录的景点回退中文名+城市。"""

# 拼音非标准英文名的城市单独映射（其余城市用 pinyin + " skyline" 即可）
CITY_QUERY = {
    "香格里拉": "Shangri-La Yunnan",
    "乌鲁木齐": "Urumqi city",
    "香港": "Hong Kong Victoria Harbour skyline",
    "澳门": "Macau city",
    "台北": "Taipei city",
}

ATTR_QUERY = {
    # 北京
    "故宫博物院": "Forbidden City Beijing",
    "长城·八达岭": "Great Wall of China Badaling",
    "颐和园": "Summer Palace Beijing",
    "天坛公园": "Temple of Heaven Beijing",
    "国家博物馆": "National Museum of China Beijing",
    "南锣鼓巷": "Nanluoguxiang Beijing hutong",
    "798艺术区": "798 Art Zone Beijing",
    "鸟巢·水立方": "Beijing National Stadium Bird's Nest",
    "圆明园遗址公园": "Old Summer Palace Yuanmingyuan Beijing",
    "什刹海": "Shichahai Beijing",
    # 上海
    "外滩": "The Bund Shanghai",
    "东方明珠": "Oriental Pearl Tower Shanghai",
    "上海迪士尼乐园": "Shanghai Disneyland",
    "豫园": "Yuyuan Garden Shanghai",
    "南京路步行街": "Nanjing Road Shanghai",
    "上海博物馆": "Shanghai Museum",
    "田子坊": "Tianzifang Shanghai",
    "朱家角古镇": "Zhujiajiao water town Shanghai",
    # 杭州
    "西湖": "West Lake Hangzhou",
    "灵隐寺": "Lingyin Temple Hangzhou",
    "西溪湿地": "Xixi National Wetland Park Hangzhou",
    "宋城景区": "Songcheng Hangzhou",
    "龙井村": "Longjing tea village Hangzhou",
    "千岛湖": "Qiandao Lake Zhejiang",
    "雷峰塔": "Leifeng Pagoda Hangzhou",
    # 苏州
    "拙政园": "Humble Administrator's Garden Suzhou",
    "虎丘": "Tiger Hill Suzhou",
    "周庄古镇": "Zhouzhuang water town",
    "留园": "Lingering Garden Suzhou",
    "平江路": "Pingjiang Road Suzhou",
    "寒山寺": "Hanshan Temple Suzhou",
    # 南京
    "中山陵": "Sun Yat-sen Mausoleum Nanjing",
    "夫子庙·秦淮河": "Confucius Temple Qinhuai River Nanjing",
    "明孝陵": "Ming Xiaoling Mausoleum Nanjing",
    "总统府": "Presidential Palace Nanjing",
    "南京博物院": "Nanjing Museum",
    "玄武湖": "Xuanwu Lake Nanjing",
    # 黄山
    "黄山风景区": "Huangshan Yellow Mountain",
    "宏村": "Hongcun village Anhui",
    "西递": "Xidi village Anhui",
    "屯溪老街": "Tunxi Old Street Huangshan",
    # 厦门
    "鼓浪屿": "Gulangyu Island Xiamen",
    "南普陀寺": "Nanputuo Temple Xiamen",
    "曾厝垵": "Zengcuoan Xiamen",
    "厦门大学": "Xiamen University",
    "环岛路": "Huandao Road Xiamen",
    # 青岛
    "栈桥": "Zhanqiao Pier Qingdao",
    "八大关": "Badaguan Qingdao",
    "崂山": "Mount Lao Qingdao",
    "五四广场": "May Fourth Square Qingdao",
    "金沙滩": "Golden Beach Qingdao",
    # 济南
    "趵突泉": "Baotu Spring Jinan",
    "大明湖": "Daming Lake Jinan",
    "千佛山": "Qianfo Mountain Jinan",
    # 无锡
    "灵山大佛": "Lingshan Grand Buddha Wuxi",
    "鼋头渚": "Yuantouzhu Wuxi",
    "拈花湾": "Nianhua Bay Wuxi",
    # 宁波
    "天一阁": "Tianyi Pavilion Ningbo",
    "溪口风景区": "Xikou Fenghua Ningbo",
    # 福州
    "三坊七巷": "Three Lanes and Seven Alleys Fuzhou",
    "鼓山": "Gushan Fuzhou",
    # 泉州
    "开元寺": "Kaiyuan Temple Quanzhou",
    "清净寺": "Qingjing Mosque Quanzhou",
    "洛阳桥": "Luoyang Bridge Quanzhou",
    # 九江
    "庐山": "Mount Lu Lushan Jiangxi",
    "鄱阳湖": "Poyang Lake Jiangxi",
    # 烟台
    "蓬莱阁": "Penglai Pavilion Yantai",
    "长岛": "Changdao Islands Yantai",
    # 威海
    "刘公岛": "Liugong Island Weihai",
    "成山头": "Chengshantou Weihai",
    # 成都
    "大熊猫繁育研究基地": "Chengdu Research Base of Giant Panda Breeding",
    "武侯祠": "Wuhou Shrine Chengdu",
    "杜甫草堂": "Du Fu Thatched Cottage Chengdu",
    "宽窄巷子": "Kuanzhai Alley Chengdu",
    "锦里古街": "Jinli Street Chengdu",
    "都江堰": "Dujiangyan Irrigation System",
    "青城山": "Mount Qingcheng Chengdu",
    "春熙路": "Chunxi Road Chengdu",
    # 重庆
    "洪崖洞": "Hongya Cave Chongqing",
    "磁器口古镇": "Ciqi Kou Chongqing",
    "长江索道": "Yangtze River Cableway Chongqing",
    "解放碑": "Jiefangbei Chongqing",
    "武隆天生三桥": "Wulong Karst Three Natural Bridges",
    "大足石刻": "Dazu Rock Carvings",
    # 昆明
    "滇池": "Dian Lake Kunming",
    "石林": "Stone Forest Shilin Kunming",
    "翠湖公园": "Green Lake Kunming",
    "云南民族村": "Yunnan Nationalities Village Kunming",
    # 丽江
    "丽江古城": "Lijiang Old Town",
    "玉龙雪山": "Jade Dragon Snow Mountain Lijiang",
    "束河古镇": "Shuhe Ancient Town Lijiang",
    "泸沽湖": "Lugu Lake Yunnan",
    "虎跳峡": "Tiger Leaping Gorge Yunnan",
    # 贵阳
    "黔灵山公园": "Qianling Park Guiyang",
    "甲秀楼": "Jiaxiu Tower Guiyang",
    "青岩古镇": "Qingyan Ancient Town Guiyang",
    "黄果树瀑布": "Huangguoshu Waterfall Guizhou",
    # 拉萨
    "布达拉宫": "Potala Palace Lhasa",
    "大昭寺": "Jokhang Temple Lhasa",
    "八廓街": "Barkhor Street Lhasa",
    "纳木错": "Namtso Lake Tibet",
    "色拉寺": "Sera Monastery Lhasa",
    # 大理
    "洱海": "Erhai Lake Dali",
    "大理古城": "Dali Old Town",
    "崇圣寺三塔": "Three Pagodas of Chongsheng Temple Dali",
    "苍山": "Cangshan Mountain Dali",
    "双廊古镇": "Shuanglang Dali",
    # 九寨沟
    "九寨沟风景区": "Jiuzhaigou Valley Sichuan",
    "黄龙景区": "Huanglong Scenic Area Sichuan",
    # 乐山
    "乐山大佛": "Leshan Giant Buddha",
    "峨眉山": "Mount Emei Sichuan",
    # 香格里拉
    "普达措国家公园": "Pudacuo National Park Shangri-La",
    "松赞林寺": "Songzanlin Monastery Shangri-La",
    # 西双版纳
    "中科院热带植物园": "Xishuangbanna Tropical Botanical Garden",
    "野象谷": "Wild Elephant Valley Xishuangbanna",
    "曼听公园": "Manting Park Xishuangbanna",
    # 天津
    "天津之眼": "Tianjin Eye Ferris wheel",
    "五大道": "Wudadao Five Avenues Tianjin",
    "古文化街": "Ancient Culture Street Tianjin",
    "瓷房子": "Porcelain House Tianjin",
    # 秦皇岛
    "山海关": "Shanhaiguan Great Wall Qinhuangdao",
    "北戴河": "Beidaihe Qinhuangdao",
    "老龙头": "Laolongtou Great Wall Qinhuangdao",
    # 大同
    "云冈石窟": "Yungang Grottoes Datong",
    "悬空寺": "Hanging Temple Hengshan",
    "华严寺": "Huayan Temple Datong",
    # 呼和浩特
    "大召寺": "Dazhao Temple Hohhot",
    "内蒙古博物院": "Inner Mongolia Museum Hohhot",
    "昭君墓": "Zhaojun Tomb Hohhot",
    # 承德
    "避暑山庄": "Chengde Mountain Resort",
    "外八庙": "Putuo Zongcheng Temple Chengde",
    # 张家口
    "草原天路": "Grassland Sky Road Zhangjiakou",
    "崇礼滑雪场": "Chongli ski resort Zhangjiakou",
    # 广州
    "广州塔": "Canton Tower Guangzhou",
    "陈家祠": "Chen Clan Ancestral Hall Guangzhou",
    "沙面岛": "Shamian Island Guangzhou",
    "白云山": "Baiyun Mountain Guangzhou",
    "上下九步行街": "Shangxiajiu Pedestrian Street Guangzhou",
    "长隆旅游度假区": "Chimelong Paradise Guangzhou",
    # 深圳
    "世界之窗": "Window of the World Shenzhen",
    "东部华侨城": "OCT East Shenzhen",
    "大梅沙海滨公园": "Dameisha Beach Shenzhen",
    "莲花山公园": "Lianhua Mountain Park Shenzhen",
    # 长沙
    "橘子洲": "Orange Isle Changsha",
    "岳麓山": "Mount Yuelu Changsha",
    "湖南省博物馆": "Hunan Provincial Museum Changsha",
    "太平老街": "Taiping Street Changsha",
    "天心阁": "Tianxin Pavilion Changsha",
    # 武汉
    "黄鹤楼": "Yellow Crane Tower Wuhan",
    "东湖风景区": "East Lake Wuhan",
    "武汉大学": "Wuhan University cherry blossom",
    "户部巷": "Hubu Alley Wuhan",
    "湖北省博物馆": "Hubei Provincial Museum Wuhan",
    "长江大桥": "Wuhan Yangtze River Bridge",
    # 桂林
    "漓江": "Li River Guilin",
    "阳朔西街": "West Street Yangshuo",
    "象鼻山": "Elephant Trunk Hill Guilin",
    "龙脊梯田": "Longji Rice Terraces Guilin",
    "两江四湖": "Two Rivers Four Lakes Guilin",
    "印象刘三姐": "Impression Liu Sanjie Guilin",
    # 三亚
    "亚龙湾": "Yalong Bay Sanya",
    "蜈支洲岛": "Wuzhizhou Island Sanya",
    "天涯海角": "Tianya Haijiao Sanya",
    "南山文化旅游区": "Nanshan Temple Sanya",
    "呀诺达雨林": "Yanoda Rainforest Sanya",
    # 郑州
    "少林寺": "Shaolin Temple Henan",
    "河南博物院": "Henan Museum Zhengzhou",
    "黄河风景名胜区": "Yellow River Scenic Area Zhengzhou",
    # 洛阳
    "龙门石窟": "Longmen Grottoes Luoyang",
    "白马寺": "White Horse Temple Luoyang",
    "洛阳博物馆": "Luoyang Museum",
    "丽景门": "Lijing Gate Luoyang",
    # 开封
    "清明上河园": "Qingming Riverside Landscape Garden Kaifeng",
    "开封府": "Kaifeng Prefecture",
    "大相国寺": "Daxiangguo Temple Kaifeng",
    # 张家界
    "武陵源": "Wulingyuan Zhangjiajie",
    "天门山": "Tianmen Mountain Zhangjiajie",
    "大峡谷玻璃桥": "Zhangjiajie Grand Canyon Glass Bridge",
    # 南宁
    "青秀山": "Qingxiu Mountain Nanning",
    "南宁中山路夜市": "Zhongshan Road Night Market Nanning",
    # 北海
    "北海银滩": "Beihai Silver Beach",
    "涠洲岛": "Weizhou Island Beihai",
    "北海老街": "Beihai Old Street",
    # 海口
    "骑楼老街": "Qilou Old Street Haikou",
    "火山口地质公园": "Haikou Volcanic Cluster Global Geopark",
    "假日海滩": "Holiday Beach Haikou",
    # 大连
    "星海广场": "Xinghai Square Dalian",
    "棒棰岛": "Bangchuidao Island Dalian",
    "老虎滩海洋公园": "Laohutan Ocean Park Dalian",
    "金石滩": "Jinshitan Dalian",
    "发现王国": "Discoveryland Dalian",
    # 沈阳
    "沈阳故宫": "Mukden Palace Shenyang",
    "张氏帅府": "Zhang Xueliang Mansion Shenyang",
    "北陵公园": "Beiling Park Shenyang",
    # 哈尔滨
    "冰雪大世界": "Harbin Ice and Snow World",
    "中央大街": "Central Street Harbin",
    "圣索菲亚教堂": "Saint Sophia Cathedral Harbin",
    "太阳岛": "Sun Island Harbin",
    "亚布力滑雪场": "Yabuli Ski Resort",
    # 长春
    "伪满皇宫博物院": "Museum of the Imperial Palace of Manchukuo Changchun",
    "净月潭": "Jingyue Lake Changchun",
    "长影世纪城": "Changchun Film Century City",
    # 西安
    "秦始皇兵马俑": "Terracotta Army Xi'an",
    "大雁塔": "Giant Wild Goose Pagoda Xi'an",
    "西安古城墙": "Xi'an City Wall",
    "回民街": "Muslim Quarter Xi'an",
    "华清宫": "Huaqing Palace Xi'an",
    "陕西历史博物馆": "Shaanxi History Museum Xi'an",
    "大唐不夜城": "Great Tang All Day Mall Xi'an",
    "华山": "Mount Hua Huashan",
    # 兰州
    "中山桥": "Zhongshan Bridge Lanzhou",
    "黄河母亲雕塑": "Yellow River Mother Sculpture Lanzhou",
    "甘肃省博物馆": "Gansu Provincial Museum Lanzhou",
    "白塔山公园": "White Pagoda Mountain Lanzhou",
    # 敦煌
    "莫高窟": "Mogao Caves Dunhuang",
    "鸣沙山月牙泉": "Crescent Lake Mingsha Shan Dunhuang",
    "雅丹魔鬼城": "Yardang National Geopark Dunhuang",
    "阳关": "Yangguan Pass Dunhuang",
    # 乌鲁木齐
    "新疆国际大巴扎": "Xinjiang International Grand Bazaar Urumqi",
    "天山天池": "Tianchi Heavenly Lake Tianshan",
    "红山公园": "Hongshan Park Urumqi",
    # 延安
    "宝塔山": "Baota Mountain Yan'an",
    "延安革命纪念馆": "Yan'an Revolutionary Memorial Hall",
    "黄帝陵": "Mausoleum of Yellow Emperor Huangling",
    # 银川
    "西夏王陵": "Western Xia Tombs Yinchuan",
    "沙湖": "Sand Lake Shahu Yinchuan",
    "镇北堡西部影城": "Zhenbeibao Western Film City",
    # 西宁
    "塔尔寺": "Kumbum Monastery Ta'er Temple",
    "青海湖": "Qinghai Lake",
    "茶卡盐湖": "Chaka Salt Lake Qinghai",
    # 吐鲁番
    "火焰山": "Flaming Mountains Turpan",
    "葡萄沟": "Grape Valley Turpan",
    "坎儿井": "Karez Turpan",
    "交河故城": "Jiaohe Ancient City Turpan",
    # 香港
    "维多利亚港": "Victoria Harbour Hong Kong",
    "太平山顶": "Victoria Peak Hong Kong",
    "香港迪士尼乐园": "Hong Kong Disneyland",
    "旺角": "Mong Kok Hong Kong",
    "星光大道": "Avenue of Stars Hong Kong",
    # 澳门
    "大三巴牌坊": "Ruins of St. Paul's Macau",
    "威尼斯人度假村": "The Venetian Macao",
    "澳门历史城区": "Historic Centre of Macau",
    "官也街": "Rua do Cunha Macau",
    # 台北
    "台北故宫博物院": "National Palace Museum Taipei",
    "台北101": "Taipei 101",
    "士林夜市": "Shilin Night Market Taipei",
    "九份老街": "Jiufen Old Street",
    "中正纪念堂": "Chiang Kai-shek Memorial Hall Taipei",
}
