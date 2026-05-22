# 后端代码更新日志

## 更新日期：2025-01-20

---

## 一、包名统一

### 修改内容
将所有 Java 包从 `com.duoduo` 统一为 `com.duoduocode.service`

### 涉及的包
| 原包名 | 新包名 |
|--------|--------|
| com.duoduo.account | com.duoduocode.service.account |
| com.duoduo.budget | com.duoduocode.service.budget |
| com.duoduo.category | com.duoduocode.service.category |
| com.duoduo.common | com.duoduocode.service.common |
| com.duoduo.config | com.duoduocode.service.config |
| com.duoduo.dashboard | com.duoduocode.service.dashboard |
| com.duoduo.dto | com.duoduocode.service.dto |
| com.duoduo.entity | com.duoduocode.service.entity |
| com.duoduo.mapper | com.duoduocode.service.mapper |
| com.duoduo.security | com.duoduocode.service.security |
| com.duoduo.service | com.duoduocode.service.service |
| com.duoduo.tag | com.duoduocode.service.tag |
| com.duoduo.transaction | com.duoduocode.service.transaction |

---

## 二、新增模块

### 1. 周期交易模板模块 (recurring)

**文件列表：**
- `recurring/entity/RecurringTemplate.java`
- `recurring/dto/RecurringTemplateDTO.java`
- `recurring/dto/RecurringTemplateVO.java`
- `recurring/mapper/RecurringTemplateMapper.java`
- `recurring/service/RecurringTemplateService.java`
- `recurring/controller/RecurringTemplateController.java`
- `resources/mapper/RecurringTemplateMapper.xml`

**API 接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/recurring-templates | 获取模板列表 |
| GET | /v1/recurring-templates/{id} | 获取模板详情 |
| POST | /v1/recurring-templates | 创建模板 |
| PUT | /v1/recurring-templates/{id} | 更新模板 |
| DELETE | /v1/recurring-templates/{id} | 删除模板 |
| GET | /v1/recurring-templates/due | 获取到期提醒列表 |
| POST | /v1/recurring-templates/{id}/trigger | 手动触发提醒 |
| POST | /v1/recurring-templates/{id}/pause | 暂停模板 |
| POST | /v1/recurring-templates/{id}/resume | 恢复模板 |

---

### 2. 投资市值记录模块 (investment)

**文件列表：**
- `investment/entity/InvestmentValue.java`
- `investment/dto/InvestmentValueDTO.java`
- `investment/dto/InvestmentValueVO.java`
- `investment/mapper/InvestmentValueMapper.java`
- `investment/service/InvestmentValueService.java`
- `investment/controller/InvestmentValueController.java`

**API 接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /v1/accounts/{accountId}/market-value | 记录市值 |
| GET | /v1/accounts/{accountId}/market-value/history | 获取市值历史 |
| GET | /v1/accounts/{accountId}/market-value/latest | 获取最新市值 |
| GET | /v1/accounts/{accountId}/market-value/profit | 计算收益 |

---

### 3. 理财收益记录模块 (investment)

**文件列表：**
- `investment/entity/InvestmentIncome.java`
- `investment/dto/InvestmentIncomeDTO.java`
- `investment/dto/InvestmentIncomeVO.java`
- `investment/mapper/InvestmentIncomeMapper.java`
- `investment/service/InvestmentIncomeService.java`
- `investment/controller/InvestmentIncomeController.java`

**API 接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /v1/accounts/{accountId}/income | 记录收益 |
| GET | /v1/accounts/{accountId}/income/history | 获取收益历史 |
| GET | /v1/accounts/{accountId}/income/monthly | 获取月度收益 |
| GET | /v1/accounts/{accountId}/income/total | 获取总收益 |

---

### 4. 报表统计模块 (report)

**文件列表：**
- `report/controller/ReportController.java`
- `report/service/ReportService.java`
- `report/mapper/ReportMapper.java`
- `report/dto/IncomeExpenseReportVO.java`
- `report/dto/AccountTrendReportVO.java`
- `report/dto/CategoryAnalysisReportVO.java`
- `report/dto/MonthlyComparisonVO.java`
- `report/dto/MonthlyData.java`
- `report/dto/CategoryData.java`
- `report/dto/DailyBalance.java`
- `resources/mapper/ReportMapper.xml`

**API 接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /v1/reports/income-expense | 收支报表 |
| GET | /v1/reports/account-trend | 账户趋势 |
| GET | /v1/reports/category-analysis | 分类分析 |
| GET | /v1/reports/monthly-comparison | 月度对比 |

---

## 三、功能增强

### 1. 交易搜索筛选功能增强

**新增文件：**
- `transaction/dto/TransactionSearchQuery.java`

**增强接口：**
- GET /v1/transactions/search

**支持筛选条件：**
- keyword: 关键词（描述模糊搜索）
- minAmount/maxAmount: 金额范围
- categoryId: 分类ID
- accountId: 账户ID
- startDate/endDate: 日期范围
- tagIds: 标签ID列表
- type: 交易类型
- sortBy/sortOrder: 排序

---

### 2. 分类迁移接口

**新增文件：**
- `category/dto/CategoryMigrateDTO.java`

**新增接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /v1/categories/{id}/migrate | 迁移分类 |
| GET | /v1/categories/{id}/usage | 获取分类使用次数 |
| GET | /v1/categories/{id}/migration-options | 获取可迁移选项 |

**迁移逻辑：**
- 将原分类的交易迁移到目标分类
- 子分类统一迁移到目标分类下
- 同名子分类自动合并

---

### 3. 预算结转接口

**新增接口：**
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /v1/budgets/daily/carryover | 执行预算结转 |
| GET | /v1/budgets/daily/carryover/history | 获取结转历史 |
| GET | /v1/budgets/daily/carryover/calculate | 计算可结转金额 |
| GET | /v1/budgets/daily/carryover/statistics | 获取结转统计 |

---

### 4. 退款接口完善

**完善内容：**
- 校验退款金额 ≤ 原交易金额
- 校验原交易类型为支出
- 校验未全额退款
- 创建退款记录
- 更新原交易退款状态
- 还原预算占用
- 更新账户余额
- 创建分录记录

---

## 四、完整 API 列表

### 认证模块
- POST /v1/auth/login
- GET /v1/auth/userinfo
- POST /v1/auth/refresh
- POST /v1/auth/logout

### 账户模块
- GET /v1/accounts
- GET /v1/accounts/{id}
- POST /v1/accounts
- PUT /v1/accounts/{id}
- DELETE /v1/accounts/{id}
- GET /v1/accounts/{id}/transactions
- GET /v1/accounts/{id}/history
- POST /v1/accounts/{id}/market-value
- GET /v1/accounts/{id}/market-value/history
- POST /v1/accounts/{id}/income
- GET /v1/accounts/{id}/income/history

### 分类模块
- GET /v1/categories
- GET /v1/categories/recent
- GET /v1/categories/search
- POST /v1/categories
- PUT /v1/categories/{id}
- DELETE /v1/categories/{id}
- POST /v1/categories/{id}/migrate
- GET /v1/categories/{id}/usage
- GET /v1/categories/{id}/migration-options

### 交易模块
- GET /v1/transactions
- GET /v1/transactions/recent
- GET /v1/transactions/search
- GET /v1/transactions/{id}
- POST /v1/transactions
- PUT /v1/transactions/{id}
- DELETE /v1/transactions/{id}
- POST /v1/transactions/{id}/refund
- POST /v1/transactions/check-duplicate

### 预算模块
- GET /v1/budgets/daily
- PUT /v1/budgets/daily
- GET /v1/budgets/daily/usage
- POST /v1/budgets/daily/carryover
- GET /v1/budgets/daily/carryover/history
- GET /v1/budgets/special
- POST /v1/budgets/special
- PUT /v1/budgets/special/{id}
- POST /v1/budgets/special/{id}/complete

### 标签模块
- GET /v1/tags
- POST /v1/tags
- PUT /v1/tags/{id}
- DELETE /v1/tags/{id}

### 周期模板模块
- GET /v1/recurring-templates
- GET /v1/recurring-templates/{id}
- POST /v1/recurring-templates
- PUT /v1/recurring-templates/{id}
- DELETE /v1/recurring-templates/{id}
- GET /v1/recurring-templates/due
- POST /v1/recurring-templates/{id}/trigger
- POST /v1/recurring-templates/{id}/pause
- POST /v1/recurring-templates/{id}/resume

### 报表模块
- GET /v1/reports/income-expense
- GET /v1/reports/account-trend
- GET /v1/reports/category-analysis
- GET /v1/reports/monthly-comparison

### 仪表盘模块
- GET /v1/dashboard
- GET /v1/dashboard/networth
- GET /v1/dashboard/monthly-summary
- GET /v1/dashboard/budget-overview

---

## 五、项目结构

```
duoduocode-service/
├── src/main/java/com/duoduocode/service/
│   ├── DuoduocodeServiceApplication.java
│   ├── account/              # 账户模块
│   ├── budget/               # 预算模块
│   ├── category/             # 分类模块
│   ├── common/               # 通用类
│   ├── config/               # 配置类
│   ├── dashboard/            # 仪表盘模块
│   ├── investment/           # 投资模块（新增）
│   ├── recurring/            # 周期模板模块（新增）
│   ├── report/               # 报表模块（新增）
│   ├── security/             # 安全模块
│   ├── tag/                  # 标签模块
│   └── transaction/          # 交易模块
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    └── mapper/               # MyBatis XML
```

---

## 六、后续建议

1. **数据库表补充**：需要补充 `recurring_template` 表的建表语句
2. **单元测试**：建议为各 Service 层添加单元测试
3. **接口文档**：建议使用 Swagger 生成 API 文档
4. **性能优化**：大数据量报表查询建议添加缓存
