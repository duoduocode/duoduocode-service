package com.duoduocode.service.billimport;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝/微信账单分类 → 系统默认分类映射（静态表 + 未命中返回 null 由「未分类」兜底）。
 */
public final class BillCategoryMapper {

    private BillCategoryMapper() {
    }

    private static final Map<String, String> MAPPING = new HashMap<>();

    static {
        // 支付宝交易分类 → 系统默认支出大类
        MAPPING.put("餐饮美食", "餐饮");
        MAPPING.put("交通出行", "交通");
        MAPPING.put("日用百货", "购物");
        MAPPING.put("服饰装扮", "购物");
        MAPPING.put("家用电器", "购物");
        MAPPING.put("家纺家装", "居住");
        MAPPING.put("家居家装", "居住");
        MAPPING.put("住房物业", "居住");
        MAPPING.put("娱乐休闲", "娱乐");
        MAPPING.put("文化休闲", "娱乐");
        MAPPING.put("运动健康", "娱乐");
        MAPPING.put("教育培训", "教育");
        MAPPING.put("医疗健康", "医疗");
        MAPPING.put("保健养生", "医疗");
        MAPPING.put("通讯物流", "通讯");
        MAPPING.put("宠物", "宠物");
        MAPPING.put("亲子", "育儿");
        MAPPING.put("育儿", "育儿");
        MAPPING.put("公益捐赠", "人情");
        MAPPING.put("转账红包", "人情");
        MAPPING.put("保险保障", "金融保险");
        MAPPING.put("金融保险", "金融保险");
        MAPPING.put("充值缴费", "其他支出");
        MAPPING.put("投资理财", "其他支出");
        // 收入方向（支付宝）
        MAPPING.put("工资", "工资");
        MAPPING.put("奖金", "工资");
        MAPPING.put("兼职", "兼职副业");
        MAPPING.put("理财收益", "投资收益");
        MAPPING.put("退款", "退款报销");
        MAPPING.put("红包", "红包礼金");
        // 微信交易类型 → 系统默认分类
        MAPPING.put("商户消费", "其他支出");
        MAPPING.put("扫二维码付款", "其他支出");
        MAPPING.put("用户转账", "人情");
        MAPPING.put("微信红包", "红包礼金");
        MAPPING.put("转账", "人情");
        MAPPING.put("零钱充值", "其他支出");
        MAPPING.put("信用卡还款", "其他支出");
        MAPPING.put("退款", "退款报销");
    }

    /**
     * 账单分类 → 系统分类名；未命中返回 null（由「未分类」兜底）
     */
    public static String map(String billCategory) {
        if (billCategory == null) {
            return null;
        }
        return MAPPING.get(billCategory.trim());
    }
}
