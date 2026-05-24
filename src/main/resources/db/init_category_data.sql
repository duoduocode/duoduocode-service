-- ============================================================
-- 分类初始化数据（默认 user_id = 1）
-- 支持两级树形结构：一级分类 + 二级分类
-- ============================================================

SET @user_id = 1;

-- ============================================================
-- 1. 支出分类 (expense)
-- ============================================================

-- 一级分类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '餐饮',   'expense', '🍜', NULL, 1,  NOW()),
(@user_id, '交通',   'expense', '🚗', NULL, 2,  NOW()),
(@user_id, '购物',   'expense', '🛒', NULL, 3,  NOW()),
(@user_id, '居住',   'expense', '🏠', NULL, 4,  NOW()),
(@user_id, '医疗',   'expense', '🏥', NULL, 5,  NOW()),
(@user_id, '育儿',   'expense', '👶', NULL, 6,  NOW()),
(@user_id, '娱乐',   'expense', '🎬', NULL, 7,  NOW()),
(@user_id, '教育',   'expense', '📚', NULL, 8,  NOW()),
(@user_id, '通讯',   'expense', '📱', NULL, 9,  NOW()),
(@user_id, '人情',   'expense', '👫', NULL, 10, NOW()),
(@user_id, '宠物',   'expense', '🐱', NULL, 11, NOW()),
(@user_id, '金融保险','expense', '🏛️', NULL, 12, NOW()),
(@user_id, '其他支出','expense', '🔧', NULL, 99, NOW());

-- 记录一级分类ID（按插入顺序）
SET @p_food       = LAST_INSERT_ID() - 12;
SET @p_transport  = @p_food + 1;
SET @p_shopping   = @p_food + 2;
SET @p_housing    = @p_food + 3;
SET @p_medical    = @p_food + 4;
SET @p_baby       = @p_food + 5;
SET @p_entertain  = @p_food + 6;
SET @p_education  = @p_food + 7;
SET @p_comm       = @p_food + 8;
SET @p_social     = @p_food + 9;
SET @p_pet        = @p_food + 10;
SET @p_finance    = @p_food + 11;
SET @p_exp_other  = @p_food + 12;

-- 二级分类：餐饮
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '早午晚餐', 'expense', '🍚', @p_food, 1, NOW()),
(@user_id, '零食水果', 'expense', '🍎', @p_food, 2, NOW()),
(@user_id, '外卖',     'expense', '🥡', @p_food, 3, NOW()),
(@user_id, '饮品咖啡', 'expense', '☕', @p_food, 4, NOW()),
(@user_id, '聚餐宴请', 'expense', '🎉', @p_food, 5, NOW());

-- 二级分类：交通
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '公交地铁', 'expense', '🚇', @p_transport, 1, NOW()),
(@user_id, '打车拼车', 'expense', '🚕', @p_transport, 2, NOW()),
(@user_id, '加油充电', 'expense', '⛽', @p_transport, 3, NOW()),
(@user_id, '停车费',   'expense', '🅿️', @p_transport, 4, NOW()),
(@user_id, '火车飞机', 'expense', '✈️', @p_transport, 5, NOW());

-- 二级分类：购物
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '服饰鞋包', 'expense', '👗', @p_shopping, 1, NOW()),
(@user_id, '数码电子', 'expense', '📱', @p_shopping, 2, NOW()),
(@user_id, '家居日用', 'expense', '🏠', @p_shopping, 3, NOW()),
(@user_id, '个人护理', 'expense', '💄', @p_shopping, 4, NOW()),
(@user_id, '其他购物', 'expense', '🛍️', @p_shopping, 5, NOW());

-- 二级分类：居住
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '房租',     'expense', '🏢', @p_housing, 1, NOW()),
(@user_id, '房贷',     'expense', '🏦', @p_housing, 2, NOW()),
(@user_id, '水电燃气', 'expense', '💡', @p_housing, 3, NOW()),
(@user_id, '物业费',   'expense', '🏘️', @p_housing, 4, NOW()),
(@user_id, '维修装修', 'expense', '🔧', @p_housing, 5, NOW()),
(@user_id, '宽带网络', 'expense', '🌐', @p_housing, 6, NOW());

-- 二级分类：医疗
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '门诊', 'expense', '🏥', @p_medical, 1, NOW()),
(@user_id, '住院', 'expense', '🏨', @p_medical, 2, NOW()),
(@user_id, '药品', 'expense', '💊', @p_medical, 3, NOW()),
(@user_id, '体检', 'expense', '🩺', @p_medical, 4, NOW());

-- 二级分类：育儿
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '奶粉尿布', 'expense', '🍼', @p_baby, 1, NOW()),
(@user_id, '教育学费', 'expense', '🏫', @p_baby, 2, NOW()),
(@user_id, '玩具',     'expense', '🧸', @p_baby, 3, NOW()),
(@user_id, '兴趣班',   'expense', '🎨', @p_baby, 4, NOW()),
(@user_id, '其他育儿', 'expense', '👶', @p_baby, 5, NOW());

-- 二级分类：娱乐
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, 'KTV电影',  'expense', '🎬', @p_entertain, 1, NOW()),
(@user_id, '旅行度假', 'expense', '✈️', @p_entertain, 2, NOW()),
(@user_id, '游戏',     'expense', '🎮', @p_entertain, 3, NOW()),
(@user_id, '运动健身', 'expense', '🏃', @p_entertain, 4, NOW()),
(@user_id, '其他娱乐', 'expense', '🎉', @p_entertain, 5, NOW());

-- 二级分类：教育
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '书籍',     'expense', '📖', @p_education, 1, NOW()),
(@user_id, '在线课程', 'expense', '💻', @p_education, 2, NOW()),
(@user_id, '文具',     'expense', '✏️', @p_education, 3, NOW());

-- 二级分类：通讯
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '手机话费', 'expense', '📞', @p_comm, 1, NOW()),
(@user_id, '快递邮寄', 'expense', '📦', @p_comm, 2, NOW());

-- 二级分类：人情
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '红包礼金', 'expense', '🧧', @p_social, 1, NOW()),
(@user_id, '请客送礼', 'expense', '🎁', @p_social, 2, NOW()),
(@user_id, '孝敬长辈', 'expense', '👴', @p_social, 3, NOW());

-- 二级分类：宠物
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '宠物食品', 'expense', '🐕', @p_pet, 1, NOW()),
(@user_id, '宠物医疗', 'expense', '🏥', @p_pet, 2, NOW()),
(@user_id, '宠物用品', 'expense', '🧹', @p_pet, 3, NOW());

-- 二级分类：金融保险
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '保险',     'expense', '🛡️', @p_finance, 1, NOW()),
(@user_id, '贷款利息', 'expense', '💰', @p_finance, 2, NOW()),
(@user_id, '手续费',   'expense', '💳', @p_finance, 3, NOW());

-- 二级分类：其他支出
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '其他支出', 'expense', '🔧', @p_exp_other, 1, NOW());


-- ============================================================
-- 2. 收入分类 (income)
-- ============================================================

-- 一级分类
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '工资',     'income', '💰', NULL, 1, NOW()),
(@user_id, '兼职副业', 'income', '💵', NULL, 2, NOW()),
(@user_id, '投资收益', 'income', '📈', NULL, 3, NOW()),
(@user_id, '红包礼金', 'income', '🎁', NULL, 4, NOW()),
(@user_id, '退款报销', 'income', '🔙', NULL, 5, NOW()),
(@user_id, '其他收入', 'income', '🔧', NULL, 99, NOW());

-- 记录收入一级分类ID
SET @p_salary       = LAST_INSERT_ID() - 5;
SET @p_parttime     = @p_salary + 1;
SET @p_investment   = @p_salary + 2;
SET @p_gift_in      = @p_salary + 3;
SET @p_refund       = @p_salary + 4;
SET @p_inc_other    = @p_salary + 5;

-- 二级分类：工资
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '基本工资', 'income', '💰', @p_salary, 1, NOW()),
(@user_id, '加班费',   'income', '🕐', @p_salary, 2, NOW()),
(@user_id, '奖金年终', 'income', '🎉', @p_salary, 3, NOW());

-- 二级分类：兼职副业
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '副业兼职', 'income', '💻', @p_parttime, 1, NOW()),
(@user_id, '自由职业', 'income', '✍️', @p_parttime, 2, NOW());

-- 二级分类：投资收益
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '理财收益', 'income', '📊', @p_investment, 1, NOW()),
(@user_id, '股息红利', 'income', '💹', @p_investment, 2, NOW()),
(@user_id, '房租收入', 'income', '🏠', @p_investment, 3, NOW());

-- 二级分类：红包礼金
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '微信红包', 'income', '🧧', @p_gift_in, 1, NOW()),
(@user_id, '节日礼金', 'income', '🎁', @p_gift_in, 2, NOW());

-- 二级分类：退款报销
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '购物退款',   'income', '🛍️', @p_refund, 1, NOW()),
(@user_id, '差旅报销',   'income', '🚗', @p_refund, 2, NOW()),
(@user_id, '餐补交通补', 'income', '🍱', @p_refund, 3, NOW()),
(@user_id, '其他退款',   'income', '🔙', @p_refund, 4, NOW());

-- 二级分类：其他收入
INSERT INTO category (user_id, name, type, icon, parent_id, sort_order, created_at) VALUES
(@user_id, '其他收入', 'income', '🔧', @p_inc_other, 1, NOW());
