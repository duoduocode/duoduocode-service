# duoduocode-service API接口文档

> 包名统一为: `com.duoduocode.service`
> 基础路径: `/v1`
> 认证方式: JWT Bearer Token

---

## 目录

1. [认证管理 (Auth)](#1-认证管理-auth)
2. [用户管理 (User)](#2-用户管理-user)
3. [交易管理 (Transactions)](#3-交易管理-transactions)
4. [分类管理 (Categories)](#4-分类管理-categories)
5. [账户管理 (Accounts)](#5-账户管理-accounts)
6. [预算管理 (Budgets)](#6-预算管理-budgets)
7. [投资管理 (Investments)](#7-投资管理-investments)
8. [报表统计 (Reports)](#8-报表统计-reports)
9. [周期交易 (Recurring)](#9-周期交易-recurring)

---

## 认证说明

### 全局认证

除以下白名单路径外，**所有接口都需要在请求头中携带 Token**：

| 白名单路径 | 说明 |
|-----------|------|
| `POST /v1/auth/login` | 微信登录 |
| `POST /v1/auth/refresh-token` | 刷新Token |
| `POST /v1/auth/logout` | 退出登录 |

### 请求头格式

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### 认证失败响应

```json
{
  "code": 20000,
  "message": "Token缺失，请先登录",
  "timestamp": 1704067200000
}
```

---

## 1. 认证管理 (Auth)

**基础路径**: `/v1/auth`
**是否需要认证**: ❌ 否（登录接口）

### 1.1 微信登录
- **URL**: `POST /v1/auth/login`
- **说明**: 前端调用 `wx.login()` 获取 code，传入后端换取 openid，自动创建新用户（如不存在），签发 JWT Token
- **请求体**:
  ```json
  {
    "code": "微信wx.login()获取的code"
  }
  ```
- **响应**:
  ```json
  {
    "code": 0,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "userId": 1,
      "openid": "oABCD1234567890...",
      "nickname": "",
      "avatarUrl": "",
      "isNewUser": true
    }
  }
  ```
- **响应字段**:
  - `token`: JWT Token，后续请求需携带
  - `userId`: 用户ID
  - `openid`: 微信OpenID
  - `nickname`: 昵称（可为空）
  - `avatarUrl`: 头像URL（可为空）
  - `isNewUser`: 是否为新用户（true=引导填写资料）

### 1.2 刷新Token
- **URL**: `POST /v1/auth/refresh-token`
- **说明**: 当 Token 即将过期时，用旧 Token 换取新 Token
- **请求体**:
  ```json
  {
    "token": "旧的JWT Token"
  }
  ```
- **响应**:
  ```json
  {
    "code": 0,
    "message": "刷新成功",
    "data": {
      "token": "新的JWT Token"
    }
  }
  ```

### 1.3 退出登录
- **URL**: `POST /v1/auth/logout`
- **说明**: JWT 无状态，退出由前端清除本地 Token
- **请求头**: ✅ 需要认证
- **响应**:
  ```json
  {
    "code": 0,
    "message": "退出成功",
    "data": null
  }
  ```

---

## 2. 用户管理 (User)

**基础路径**: `/v1/user`
**是否需要认证**: ✅ 是

### 2.1 获取当前用户资料
- **URL**: `GET /v1/user/profile`
- **说明**: 获取当前登录用户的完整资料
- **响应**:
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "id": 1,
      "nickname": "小明",
      "avatarUrl": "https://example.com/avatar.png",
      "phone": "13800138000",
      "email": "user@example.com",
      "gender": 1,
      "createdAt": "2024-01-01T10:00:00"
    }
  }
  ```
- **响应字段**:
  - `id`: 用户ID
  - `nickname`: 昵称
  - `avatarUrl`: 头像URL
  - `phone`: 手机号
  - `email`: 邮箱
  - `gender`: 性别（0-未知, 1-男, 2-女）
  - `createdAt`: 注册时间

### 2.2 更新用户资料（批量）
- **URL**: `PUT /v1/user/profile`
- **说明**: 批量更新用户资料，支持同时更新多个字段
- **请求体**:
  ```json
  {
    "nickname": "新昵称",
    "avatarUrl": "https://example.com/new-avatar.png",
    "gender": 2,
    "phone": "13800138000",
    "email": "new@example.com"
  }
  ```
- **响应**:
  ```json
  {
    "code": 0,
    "message": "更新成功",
    "data": null
  }
  ```

### 2.3 单独更新昵称
- **URL**: `PUT /v1/user/nickname`
- **请求体**:
  ```json
  {
    "nickname": "新昵称"
  }
  ```
- **响应**:
  ```json
  {
    "code": 0,
    "message": "昵称更新成功",
    "data": null
  }
  ```
- **校验规则**:
  - 昵称不能为空
  - 昵称不能超过30个字符

### 2.4 单独更新头像
- **URL**: `PUT /v1/user/avatar`
- **说明**: 更新用户头像URL（需先上传到云存储或OSS）
- **请求体**:
  ```json
  {
    "avatarUrl": "https://example.com/new-avatar.png"
  }
  ```
- **响应**:
  ```json
  {
    "code": 0,
    "message": "头像更新成功",
    "data": null
  }
  ```

---

## 3. 交易管理 (Transactions)

**基础路径**: `/v1/transactions`
**是否需要认证**: ✅ 是

### 3.1 获取交易列表
- **URL**: `GET /v1/transactions`
- **参数**:
  - `page`: 页码 (默认1)
  - `pageSize`: 每页数量 (默认20)
  - `startDate`: 开始日期 (YYYY-MM-DD)
  - `endDate`: 结束日期 (YYYY-MM-DD)
- **响应**: `PageResult<TransactionVO>`

### 3.2 获取最近交易
- **URL**: `GET /v1/transactions/recent`
- **参数**:
  - `limit`: 限制条数 (默认10)
- **响应**: `List<TransactionVO>`

### 3.3 获取交易详情
- **URL**: `GET /v1/transactions/{id}`
- **参数**:
  - `id`: 交易ID (路径参数)
- **响应**: `TransactionVO`

### 3.4 创建交易
- **URL**: `POST /v1/transactions`
- **请求体**: `TransactionDTO`
  - `amount`: 金额 (必填)
  - `date`: 日期
  - `time`: 时间
  - `description`: 描述
  - `mode`: 模式 (simple/full)
  - `transactionType`: 交易类型 (expense/income/transfer/repayment)
  - `accountId`: 账户ID
  - `targetAccountId`: 目标账户ID (转账用)
  - `categoryId`: 分类ID
  - `entries`: 分录列表 (完整模式)
- **响应**: `Long` (交易ID)

### 3.5 更新交易
- **URL**: `PUT /v1/transactions/{id}`
- **参数**:
  - `id`: 交易ID (路径参数)
- **请求体**: `TransactionDTO`
- **响应**: `Void`

### 3.6 删除交易
- **URL**: `DELETE /v1/transactions/{id}`
- **参数**:
  - `id`: 交易ID (路径参数)
- **响应**: `Void`

### 3.7 退款处理
- **URL**: `POST /v1/transactions/{id}/refund`
- **参数**:
  - `id`: 交易ID (路径参数)
- **请求体**: `RefundDTO`
  - `amount`: 退款金额
  - `description`: 退款说明
  - `accountId`: 退款入账账户
- **响应**: `Void`

### 3.8 重复交易检测
- **URL**: `POST /v1/transactions/check-duplicate`
- **请求体**: `DuplicateCheckDTO`
  - `amount`: 金额
  - `date`: 日期
  - `description`: 描述
- **响应**: `Map<String, Object>`
  - `isDuplicate`: 是否重复
  - `existingTransaction`: 已存在交易

### 3.9 搜索筛选交易
- **URL**: `GET /v1/transactions/search`
- **参数**:
  - `keyword`: 关键词
  - `minAmount`: 最小金额
  - `maxAmount`: 最大金额
  - `categoryId`: 分类ID
  - `accountId`: 账户ID
  - `startDate`: 开始日期
  - `endDate`: 结束日期
  - `type`: 交易类型
  - `sortBy`: 排序字段
  - `sortOrder`: 排序方向
  - `page`: 页码
  - `pageSize`: 每页数量
- **响应**: `PageResult<TransactionVO>`

---

## 4. 分类管理 (Categories)

**基础路径**: `/v1/categories`
**是否需要认证**: ✅ 是

### 4.1 获取分类列表
- **URL**: `GET /v1/categories`
- **参数**:
  - `type`: 分类类型 (expense/income)
- **响应**: `List<CategoryTreeVO>` (树形结构)

### 4.2 获取最近使用分类
- **URL**: `GET /v1/categories/recent`
- **参数**:
  - `type`: 分类类型
  - `limit`: 返回数量 (默认10)
- **响应**: `List<CategoryVO>`

### 4.3 搜索分类
- **URL**: `GET /v1/categories/search`
- **参数**:
  - `keyword`: 搜索关键词 (必填)
- **响应**: `List<CategoryVO>`

### 4.4 获取快捷二级分类
- **URL**: `GET /v1/categories/{parentId}/quick-children`
- **参数**:
  - `parentId`: 父分类ID (路径参数)
- **响应**: `List<CategoryTreeVO>`

### 4.5 创建分类
- **URL**: `POST /v1/categories`
- **请求体**: `CategoryDTO`
  - `name`: 分类名称 (必填)
  - `type`: 分类类型 (expense/income)
  - `icon`: 图标emoji
  - `parentId`: 父分类ID
  - `monthlyBudget`: 月度预算
  - `weeklyBudget`: 周预算
  - `alertThreshold`: 预警阈值
- **响应**: `Long` (分类ID)

### 4.6 更新分类
- **URL**: `PUT /v1/categories/{id}`
- **参数**:
  - `id`: 分类ID (路径参数)
- **请求体**: `CategoryDTO`
- **响应**: `Void`

### 4.7 删除分类
- **URL**: `DELETE /v1/categories/{id}`
- **参数**:
  - `id`: 分类ID (路径参数)
  - `migrateToId`: 迁移目标分类ID (可选)
- **响应**: `Void`

### 4.8 迁移分类
- **URL**: `POST /v1/categories/{id}/migrate`
- **参数**:
  - `id`: 分类ID (路径参数)
- **请求体**: `CategoryMigrateDTO`
  - `targetCategoryId`: 目标分类ID
  - `migrateTransactions`: 是否迁移交易
  - `deleteTransactions`: 是否删除交易
- **响应**: `Void`

### 4.9 获取分类使用次数
- **URL**: `GET /v1/categories/{id}/usage`
- **参数**:
  - `id`: 分类ID (路径参数)
- **响应**: `Map<String, Object>`
  - `count`: 使用次数

### 4.10 获取可迁移目标分类
- **URL**: `GET /v1/categories/{id}/migration-options`
- **参数**:
  - `id`: 分类ID (路径参数)
- **响应**: `List<CategoryVO>`

---

## 5. 账户管理 (Accounts)

**基础路径**: `/v1/accounts`
**是否需要认证**: ✅ 是

### 5.1 获取账户列表
- **URL**: `GET /v1/accounts`
- **响应**: `Map<String, Object>`
  - `summary`: 账户汇总
    - `totalAssets`: 总资产
    - `totalLiabilities`: 总负债
    - `totalInvestments`: 总投资
    - `netWorth`: 净资产
  - `accounts`: 按类型分组的账户列表

### 5.2 获取账户详情
- **URL**: `GET /v1/accounts/{id}`
- **参数**:
  - `id`: 账户ID (路径参数)
- **响应**: `AccountVO`

### 5.3 创建账户
- **URL**: `POST /v1/accounts`
- **请求体**: `AccountDTO`
  - `name`: 账户名称 (必填)
  - `type`: 账户类型 (asset/liability/investment)
  - `icon`: 图标emoji
  - `color`: 颜色
  - `initialBalance`: 初始余额
  - `creditLimit`: 信用额度
  - `includeInNetWorth`: 计入净资产
  - `allowTransfer`: 允许转账
  - `enableAlert`: 启用预警
  - `alertThreshold`: 预警阈值
- **响应**: `Long` (账户ID)

### 5.4 更新账户
- **URL**: `PUT /v1/accounts/{id}`
- **参数**:
  - `id`: 账户ID (路径参数)
- **请求体**: `AccountDTO`
- **响应**: `Void`

### 5.5 删除账户
- **URL**: `DELETE /v1/accounts/{id}`
- **参数**:
  - `id`: 账户ID (路径参数)
- **响应**: `Void`

### 5.6 获取账户交易流水
- **URL**: `GET /v1/accounts/{id}/transactions`
- **参数**:
  - `id`: 账户ID (路径参数)
  - `page`: 页码
  - `pageSize`: 每页数量
- **响应**: `PageResult<TransactionVO>`

### 5.7 调整账户余额
- **URL**: `POST /v1/accounts/{id}/adjust-balance`
- **参数**:
  - `id`: 账户ID (路径参数)
- **请求体**: `Map<String, Object>`
  - `newBalance`: 新余额
  - `reason`: 调整原因
- **响应**: `Void`

---

## 6. 预算管理 (Budgets)

**基础路径**: `/v1/budgets`
**是否需要认证**: ✅ 是

### 6.1 获取日常预算
- **URL**: `GET /v1/budgets/daily`
- **参数**:
  - `month`: 月份 (YYYY-MM)
- **响应**: `List<DailyBudgetVO>`

### 6.2 设置日常预算
- **URL**: `PUT /v1/budgets/daily`
- **请求体**: `DailyBudgetDTO`
  - `categoryId`: 分类ID
  - `month`: 月份
  - `monthlyBudget`: 月度预算
  - `weeklyBudget`: 周预算
  - `alertThreshold`: 预警阈值
- **响应**: `Void`

### 6.3 获取预算使用情况
- **URL**: `GET /v1/budgets/daily/usage`
- **参数**:
  - `month`: 月份 (YYYY-MM)
- **响应**: `List<DailyBudgetVO>`

### 6.4 执行预算结转
- **URL**: `POST /v1/budgets/daily/carryover`
- **请求体**: `CarryoverDTO`
  - `fromMonth`: 来源月份
  - `toMonth`: 目标月份
  - `categoryCarryovers`: 分类结转列表
- **响应**: `Long` (结转记录ID)

### 6.5 获取结转历史
- **URL**: `GET /v1/budgets/daily/carryover/history`
- **参数**:
  - `month`: 月份 (YYYY-MM)
- **响应**: `List<Map<String, Object>>`

### 6.6 计算可结转金额
- **URL**: `GET /v1/budgets/daily/carryover/calculate`
- **参数**:
  - `categoryId`: 分类ID
  - `fromMonth`: 来源月份
- **响应**: `BigDecimal`

### 6.7 获取结转统计
- **URL**: `GET /v1/budgets/daily/carryover/statistics`
- **参数**:
  - `month`: 月份 (YYYY-MM)
- **响应**: `Map<String, Object>`

### 6.8 获取专项预算列表
- **URL**: `GET /v1/budgets/special`
- **响应**: `List<SpecialBudgetVO>`

### 6.9 获取专项预算详情
- **URL**: `GET /v1/budgets/special/{id}`
- **参数**:
  - `id`: 预算ID (路径参数)
- **响应**: `SpecialBudgetVO`

### 6.10 创建专项预算
- **URL**: `POST /v1/budgets/special`
- **请求体**: `SpecialBudgetDTO`
  - `name`: 预算名称 (必填)
  - `totalAmount`: 总金额 (必填)
  - `startDate`: 开始日期
  - `endDate`: 结束日期
  - `categoryIds`: 关联分类ID列表
  - `note`: 备注
- **响应**: `Long` (预算ID)

### 6.11 更新专项预算
- **URL**: `PUT /v1/budgets/special/{id}`
- **参数**:
  - `id`: 预算ID (路径参数)
- **请求体**: `SpecialBudgetDTO`
- **响应**: `Void`

### 6.12 结束专项预算
- **URL**: `POST /v1/budgets/special/{id}/complete`
- **参数**:
  - `id`: 预算ID (路径参数)
- **响应**: `Void`

### 6.13 获取可用专项预算
- **URL**: `GET /v1/budgets/special/available`
- **响应**: `List<SpecialBudgetVO>`

---

## 7. 投资管理 (Investments)

**基础路径**: `/v1/accounts` (投资账户相关)
**是否需要认证**: ✅ 是

### 7.1 记录理财收益
- **URL**: `POST /v1/accounts/{accountId}/income`
- **参数**:
  - `accountId`: 账户ID (路径参数)
- **请求体**: `InvestmentIncomeDTO`
  - `amount`: 收益金额
  - `date`: 日期
  - `note`: 备注
  - `reinvested`: 是否再投资
- **响应**: `Long` (记录ID)

### 7.2 获取收益历史
- **URL**: `GET /v1/accounts/{accountId}/income/history`
- **参数**:
  - `accountId`: 账户ID (路径参数)
  - `startDate`: 开始日期
  - `endDate`: 结束日期
- **响应**: `List<InvestmentIncomeVO>`

### 7.3 获取月度收益
- **URL**: `GET /v1/accounts/{accountId}/income/monthly`
- **参数**:
  - `accountId`: 账户ID (路径参数)
  - `month`: 月份 (YYYY-MM)
- **响应**: `Map<String, Object>`

### 7.4 获取总收益
- **URL**: `GET /v1/accounts/{accountId}/income/total`
- **参数**:
  - `accountId`: 账户ID (路径参数)
- **响应**: `Map<String, Object>`

### 7.5 记录市值
- **URL**: `POST /v1/accounts/{accountId}/market-value`
- **参数**:
  - `accountId`: 账户ID (路径参数)
- **请求体**: `InvestmentValueDTO`
  - `marketValue`: 市值
  - `costBasis`: 成本
  - `date`: 日期
  - `note`: 备注
- **响应**: `Long` (记录ID)

### 7.6 获取市值历史
- **URL**: `GET /v1/accounts/{accountId}/market-value/history`
- **参数**:
  - `accountId`: 账户ID (路径参数)
  - `startDate`: 开始日期
  - `endDate`: 结束日期
- **响应**: `List<InvestmentValueVO>`

### 7.7 获取最新市值
- **URL**: `GET /v1/accounts/{accountId}/market-value/latest`
- **参数**:
  - `accountId`: 账户ID (路径参数)
- **响应**: `InvestmentValueVO`

### 7.8 计算收益
- **URL**: `GET /v1/accounts/{accountId}/market-value/profit`
- **参数**:
  - `accountId`: 账户ID (路径参数)
- **响应**: `Map<String, Object>`

---

## 8. 报表统计 (Reports)

**基础路径**: `/v1/reports`
**是否需要认证**: ✅ 是

### 8.1 收支报表
- **URL**: `GET /v1/reports/income-expense`
- **参数**:
  - `startDate`: 开始日期 (必填)
  - `endDate`: 结束日期 (必填)
- **响应**: `IncomeExpenseReportVO`
  - `period`: 统计周期
  - `totalIncome`: 总收入
  - `totalExpense`: 总支出
  - `netAmount`: 净收支
  - `dailyData`: 每日数据列表
  - `monthlyData`: 月度数据列表

### 8.2 账户趋势
- **URL**: `GET /v1/reports/account-trend`
- **参数**:
  - `accountId`: 账户ID (必填)
  - `startDate`: 开始日期 (必填)
  - `endDate`: 结束日期 (必填)
- **响应**: `AccountTrendReportVO`
  - `accountId`: 账户ID
  - `accountName`: 账户名称
  - `startBalance`: 期初余额
  - `endBalance`: 期末余额
  - `changeAmount`: 变动金额
  - `dailyBalances`: 每日余额列表

### 8.3 分类分析
- **URL**: `GET /v1/reports/category-analysis`
- **参数**:
  - `type`: 类型 (expense/income) (必填)
  - `startDate`: 开始日期 (必填)
  - `endDate`: 结束日期 (必填)
- **响应**: `List<CategoryAnalysisReportVO>`
  - `categoryId`: 分类ID
  - `categoryName`: 分类名称
  - `categoryIcon`: 分类图标
  - `amount`: 金额
  - `percentage`: 占比
  - `transactionCount`: 交易笔数

### 8.4 月度对比
- **URL**: `GET /v1/reports/monthly-comparison`
- **参数**:
  - `month1`: 月份1 (YYYY-MM) (必填)
  - `month2`: 月份2 (YYYY-MM) (必填)
- **响应**: `MonthlyComparisonVO`
  - `month1`: 月份1数据
  - `month2`: 月份2数据
  - `incomeChange`: 收入变化
  - `expenseChange`: 支出变化

---

## 9. 周期交易 (Recurring)

**基础路径**: `/v1/recurring-templates`
**是否需要认证**: ✅ 是

### 9.1 获取周期模板列表
- **URL**: `GET /v1/recurring-templates`
- **响应**: `List<RecurringTemplateVO>`

### 9.2 获取模板详情
- **URL**: `GET /v1/recurring-templates/{id}`
- **参数**:
  - `id`: 模板ID (路径参数)
- **响应**: `RecurringTemplateVO`

### 9.3 创建模板
- **URL**: `POST /v1/recurring-templates`
- **请求体**: `RecurringTemplateDTO`
  - `name`: 模板名称
  - `amount`: 金额
  - `type`: 类型
  - `categoryId`: 分类ID
  - `accountId`: 账户ID
  - `frequency`: 频率 (daily/weekly/monthly/yearly)
  - `startDate`: 开始日期
  - `endDate`: 结束日期
- **响应**: `Long` (模板ID)

### 9.4 更新模板
- **URL**: `PUT /v1/recurring-templates/{id}`
- **参数**:
  - `id`: 模板ID (路径参数)
- **请求体**: `RecurringTemplateDTO`
- **响应**: `Void`

### 9.5 删除模板
- **URL**: `DELETE /v1/recurring-templates/{id}`
- **参数**:
  - `id`: 模板ID (路径参数)
- **响应**: `Void`

### 9.6 获取到期提醒列表
- **URL**: `GET /v1/recurring-templates/due`
- **响应**: `List<RecurringTemplateVO>`

### 9.7 手动触发提醒
- **URL**: `POST /v1/recurring-templates/{id}/trigger`
- **参数**:
  - `id`: 模板ID (路径参数)
- **响应**: `Void`

### 9.8 暂停模板
- **URL**: `POST /v1/recurring-templates/{id}/pause`
- **参数**:
  - `id`: 模板ID (路径参数)
- **响应**: `Void`

### 9.9 恢复模板
- **URL**: `POST /v1/recurring-templates/{id}/resume`
- **参数**:
  - `id`: 模板ID (路径参数)
- **响应**: `Void`

---

## 通用响应格式

### 成功响应
```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": "2024-01-01T00:00:00.000Z"
}
```

### 错误响应
```json
{
  "code": 10001,
  "message": "参数错误",
  "timestamp": "2024-01-01T00:00:00.000Z"
}
```

### 分页响应
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [ ... ],
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "hasMore": true
  }
}
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 10000 | 系统错误 |
| 10001 | 参数错误 |
| 10002 | 参数缺失 |
| 10003 | 参数格式错误 |
| 10006 | 数据不存在 |
| 20000 | 未登录或登录已过期 |
| 20001 | Token无效 |
| 20002 | Token已过期 |
| 20003 | Token缺失 |
| 20004 | 登录失败 |
| 20005 | 账号不存在 |
| 20006 | 密码错误 |
| 30000 | 业务处理失败 |
| 30001 | 用户不存在 |
| 30002 | 用户已存在 |
| 30003 | 借贷不平衡 |
| 30004 | 退款金额超过可退金额 |
| 30008 | 只有支出类型可以退款 |

---

## 前端对接流程

### 登录流程
```
1. 前端调用 wx.login() 获取 code
2. POST /v1/auth/login { code }
   ↓
   后端调用微信接口 code2Session 获取 openid
   后端查询/创建用户记录
   后端签发 JWT Token
   ↓
3. 返回 { token, userId, isNewUser, ... }
4. 前端存储 token: wx.setStorageSync('token', token)
```

### 新用户引导流程
```
1. if isNewUser === true
   ↓
2. 显示头像选择页面
   - 使用 <button open-type="chooseAvatar">
   - 获取临时头像路径
   ↓
3. PUT /v1/user/avatar { avatarUrl } 更新头像
   ↓
4. 用户输入昵称
   PUT /v1/user/nickname { nickname }
   ↓
5. 资料完善完成
```

### 后续请求流程
```
1. 每次请求前从缓存读取 token
2. 在请求头中携带: Authorization: Bearer {token}
3. 如果收到 20000 (未登录)，重新登录
```

---

## API 统计

| 模块 | 接口数量 | 需要认证 |
|------|:--------:|:--------:|
| 认证管理 | 3 | ❌ |
| 用户管理 | 4 | ✅ |
| 交易管理 | 9 | ✅ |
| 分类管理 | 10 | ✅ |
| 账户管理 | 7 | ✅ |
| 预算管理 | 13 | ✅ |
| 投资管理 | 8 | ✅ |
| 报表统计 | 4 | ✅ |
| 周期交易 | 9 | ✅ |
| **总计** | **67** | — |

---

*文档版本: 2.0*
*更新日期: 2024-01*
