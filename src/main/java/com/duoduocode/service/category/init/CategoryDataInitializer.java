package com.duoduocode.service.category.init;

import com.duoduocode.service.category.entity.Category;
import com.duoduocode.service.category.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类数据初始�? * 为新用户初始化常用收支分类（支出13大类+收入6大类，含二级分类）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryDataInitializer {

    private final CategoryMapper categoryMapper;

    /**
     * 为新用户初始化默认分类（幂等，已有则跳过）
     */
    public void initForUser(Long userId) {
        List<Category> existing = categoryMapper.selectByUserId(userId);
        if (existing != null && !existing.isEmpty()) {
            log.info("用户 {} 分类数据已存在，跳过初始化 (已有 {} 条)", userId, existing.size());
            return;
        }

        log.info("为用户 {} 初始化默认分类数据...", userId);
        Map<String, Long> parentIdMap = new LinkedHashMap<>();

        initExpenseParents(userId, parentIdMap);
        initExpenseChildren(userId, parentIdMap);

        initIncomeParents(userId, parentIdMap);
        initIncomeChildren(userId, parentIdMap);

        List<Category> all = categoryMapper.selectByUserId(userId);
        log.info("用户 {} 默认分类初始化完成，共 {} 条 (支出+收入)", userId, all != null ? all.size() : 0);
    }

    private void initExpenseParents(Long userId, Map<String, Long> idMap) {
        String[][] parents = {
            {"餐饮",       "🍜", "1"},
            {"交通",       "🚗", "2"},
            {"购物",       "🛒", "3"},
            {"居住",       "🏠", "4"},
            {"医疗",       "🏥", "5"},
            {"育儿",       "👶", "6"},
            {"娱乐",       "🎬", "7"},
            {"教育",       "📚", "8"},
            {"通讯",       "📱", "9"},
            {"人情",       "👫", "10"},
            {"宠物",       "🐱", "11"},
            {"金融保险",   "🏛️", "12"},
            {"其他支出",   "🔧", "99"},
        };
        for (String[] p : parents) {
            idMap.put(p[0], insertCategory(userId, p[0], "expense", p[1], null, Integer.parseInt(p[2])));
        }
    }

    private void initExpenseChildren(Long userId, Map<String, Long> idMap) {
        String[][][] children = {
            {{"早午晚餐","🍚","1"}, {"零食水果","🍎","2"}, {"外卖","🥡","3"}, {"饮品咖啡","☕","4"}, {"聚餐宴请","🎉","5"}},
            {{"公交地铁","🚇","1"}, {"打车拼车","🚕","2"}, {"加油充电","⛽","3"}, {"停车费","🅿️","4"}, {"火车飞机","✈️","5"}},
            {{"服饰鞋包","👗","1"}, {"数码电子","📱","2"}, {"家居日用","🏠","3"}, {"个人护理","💄","4"}, {"其他购物","🛍️","5"}},
            {{"房租","🏢","1"}, {"房贷","🏦","2"}, {"水电燃气","💡","3"}, {"物业费","🏘️","4"}, {"维修装修","🔧","5"}, {"宽带网络","🌐","6"}},
            {{"门诊","🏥","1"}, {"住院","🏨","2"}, {"药品","💊","3"}, {"体检","🩺","4"}},
            {{"奶粉尿布","🍼","1"}, {"教育学费","🏫","2"}, {"玩具","🧸","3"}, {"兴趣班","🎨","4"}, {"其他育儿","👶","5"}},
            {{"KTV电影","🎬","1"}, {"旅行度假","✈️","2"}, {"游戏","🎮","3"}, {"运动健身","🏃","4"}, {"其他娱乐","🎉","5"}},
            {{"书籍","📖","1"}, {"在线课程","💻","2"}, {"文具","✏️","3"}},
            {{"手机话费","📞","1"}, {"快递邮寄","📦","2"}},
            {{"红包礼金","🧧","1"}, {"请客送礼","🎁","2"}, {"孝敬长辈","👴","3"}},
            {{"宠物食品","🐕","1"}, {"宠物医疗","🏥","2"}, {"宠物用品","🧹","3"}},
            {{"保险","🛡️","1"}, {"贷款利息","💰","2"}, {"手续费","💳","3"}},
            {{"其他支出","🔧","1"}},
        };
        String[] parentKeys = {"餐饮","交通","购物","居住","医疗","育儿","娱乐","教育","通讯","人情","宠物","金融保险","其他支出"};
        for (int i = 0; i < children.length; i++) {
            Long parentId = idMap.get(parentKeys[i]);
            for (String[] c : children[i]) {
                insertCategory(userId, c[0], "expense", c[1], parentId, Integer.parseInt(c[2]));
            }
        }
    }

    private void initIncomeParents(Long userId, Map<String, Long> idMap) {
        String[][] parents = {
            {"工资",       "💰", "1"},
            {"兼职副业",   "💵", "2"},
            {"投资收益",   "📈", "3"},
            {"红包礼金",   "🎁", "4"},
            {"退款报销",   "🔙", "5"},
            {"其他收入",   "🔧", "99"},
        };
        for (String[] p : parents) {
            idMap.put(p[0], insertCategory(userId, p[0], "income", p[1], null, Integer.parseInt(p[2])));
        }
    }

    private void initIncomeChildren(Long userId, Map<String, Long> idMap) {
        String[][][] children = {
            {{"基本工资","💰","1"}, {"加班费","🕐","2"}, {"奖金年终","🎉","3"}},
            {{"副业兼职","💻","1"}, {"自由职业","✍️","2"}},
            {{"理财收益","📊","1"}, {"股息红利","💹","2"}, {"房租收入","🏠","3"}},
            {{"微信红包","🧧","1"}, {"节日礼金","🎁","2"}},
            {{"购物退款","🛍️","1"}, {"差旅报销","🚗","2"}, {"餐补交通补","🍱","3"}, {"其他退款","🔙","4"}},
            {{"其他收入","🔧","1"}},
        };
        String[] parentKeys = {"工资","兼职副业","投资收益","红包礼金","退款报销","其他收入"};
        for (int i = 0; i < children.length; i++) {
            Long parentId = idMap.get(parentKeys[i]);
            for (String[] c : children[i]) {
                insertCategory(userId, c[0], "income", c[1], parentId, Integer.parseInt(c[2]));
            }
        }
    }

    private Long insertCategory(Long userId, String name, String type, String icon, Long parentId, int sortOrder) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(name);
        category.setType(type);
        category.setIcon(icon);
        category.setParentId(parentId);
        category.setSortOrder(sortOrder);
        category.setIsDeleted(false);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return category.getId();
    }
}
