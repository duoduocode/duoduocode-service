# 多多记账 API 接口文档

> 版本: 1.0.0
> Swagger UI: http://localhost:8080/swagger-ui.html
> 总接口数: 74

## å¨æäº¤ææ¨¡æ¿

| 方法 | 路径 | 描述 |
|------|------|------|
| DELETE | `/v1/recurring-templates/{id}` | å é¤æ¨¡æ¿ |
| GET | `/v1/recurring-templates` | è·åå¨ææ¨¡æ¿åè¡¨ |
| GET | `/v1/recurring-templates/{id}` | è·åæ¨¡æ¿è¯¦æ |
| GET | `/v1/recurring-templates/due` | è·åå°ææéåè¡¨ |
| POST | `/v1/recurring-templates` | åå»ºæ¨¡æ¿ |
| POST | `/v1/recurring-templates/{id}/pause` | æåæ¨¡æ¿ |
| POST | `/v1/recurring-templates/{id}/resume` | æ¢å¤æ¨¡æ¿ |
| POST | `/v1/recurring-templates/{id}/trigger` | æå¨è§¦åæé |
| PUT | `/v1/recurring-templates/{id}` | æ´æ°æ¨¡æ¿ |

## åç±»ç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| DELETE | `/v1/categories/{id}` | å é¤åç±» |
| GET | `/v1/categories` | è·ååç±»åè¡¨ |
| GET | `/v1/categories/{id}/migration-options` | è·åè¿ç§»ç®æ éé¡¹ |
| GET | `/v1/categories/{id}/usage` | è·ååç±»ä½¿ç¨æ¬¡æ° |
| GET | `/v1/categories/{parentId}/quick-children` | è·åå¿«æ·äºçº§åç±» |
| GET | `/v1/categories/recent` | è·åæè¿ä½¿ç¨åç±» |
| GET | `/v1/categories/search` | æç´¢åç±» |
| POST | `/v1/categories` | åå»ºåç±» |
| POST | `/v1/categories/{id}/migrate` | è¿ç§»åç±» |
| PUT | `/v1/categories/{id}` | æ´æ°åç±» |

## æ¥è¡¨ç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/v1/reports/account-trend` | è·åè´¦æ·è¶å¿æ¥è¡¨ |
| GET | `/v1/reports/category-analysis` | è·ååç±»åææ¥è¡¨ |
| GET | `/v1/reports/income-expense` | è·åæ¶æ¯æ¥è¡¨ |
| GET | `/v1/reports/monthly-comparison` | è·åæåº¦å¯¹æ¯æ¥è¡¨ |

## æèµå¸å¼ç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/v1/accounts/{accountId}/market-value/history` | è·åå¸å¼åå² |
| GET | `/v1/accounts/{accountId}/market-value/latest` | è·åææ°å¸å¼ |
| GET | `/v1/accounts/{accountId}/market-value/profit` | è®¡ç®æ¶ç |
| POST | `/v1/accounts/{accountId}/market-value` | è®°å½å¸å¼ |

## äº¤æç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| DELETE | `/v1/transactions/{id}` | å é¤äº¤æ |
| GET | `/v1/transactions` | è·åäº¤æåè¡¨ |
| GET | `/v1/transactions/{id}` | è·åäº¤æè¯¦æ |
| GET | `/v1/transactions/recent` | è·åæè¿äº¤æ |
| GET | `/v1/transactions/search` | æç´¢ç­éäº¤æ |
| POST | `/v1/transactions` | åå»ºäº¤æ |
| POST | `/v1/transactions/{id}/refund` | éæ¬¾å¤ç |
| POST | `/v1/transactions/check-duplicate` | éå¤äº¤ææ£æµ |
| PUT | `/v1/transactions/{id}` | æ´æ°äº¤æ |

## ç¨æ·ç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/v1/user/profile` | è·åç¨æ·èµæ |
| GET | `/v1/user/stats` | è·åç»è®¡ä¿¡æ¯ |
| PUT | `/v1/user/avatar` | æ´æ°å¤´å |
| PUT | `/v1/user/nickname` | æ´æ°æµç§° |
| PUT | `/v1/user/profile` | æ´æ°ç¨æ·èµæ |

## çè´¢æ¶çç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/v1/accounts/{accountId}/income/history` | è·åæ¶çåå² |
| GET | `/v1/accounts/{accountId}/income/monthly` | è·åæåº¦æ¶ç |
| GET | `/v1/accounts/{accountId}/income/total` | è·åæ»æ¶ç |
| POST | `/v1/accounts/{accountId}/income` | è®°å½æ¶ç |

## é¦é¡µçæ¿

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/v1/dashboard` | è·åé¦é¡µçæ¿ |
| GET | `/v1/dashboard/monthly` | è·åæåº¦æ¶æ¯è¯¦æ |
| GET | `/v1/dashboard/net-worth-trend` | è·ååèµäº§è¶å¿ |
| GET | `/v1/dashboard/summary` | è·åé¦é¡µçæ¿æè¦ |
| GET | `/v1/dashboard/today` | è·åä»æ¥æ°æ® |

## è´¦æ·ç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| DELETE | `/v1/accounts/{id}` | å é¤è´¦æ· |
| GET | `/v1/accounts` | è·åè´¦æ·åè¡¨ |
| GET | `/v1/accounts/{id}` | è·åè´¦æ·è¯¦æ |
| GET | `/v1/accounts/{id}/transactions` | è·åè´¦æ·äº¤ææµæ°´ |
| POST | `/v1/accounts` | åå»ºè´¦æ· |
| POST | `/v1/accounts/{id}/adjust-balance` | è°æ´è´¦æ·ä½é¢ |
| PUT | `/v1/accounts/{id}` | æ´æ°è´¦æ· |

## é¢ç®ç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/v1/budgets/daily` | è·åæ¥å¸¸é¢ç® |
| GET | `/v1/budgets/daily/carryover/calculate` | è®¡ç®å¯ç»è½¬éé¢ |
| GET | `/v1/budgets/daily/carryover/history` | è·åç»è½¬åå² |
| GET | `/v1/budgets/daily/carryover/statistics` | è·åç»è½¬ç»è®¡ |
| GET | `/v1/budgets/daily/usage` | è·åé¢ç®ä½¿ç¨æåµ |
| GET | `/v1/budgets/special` | è·åä¸é¡¹é¢ç®åè¡¨ |
| GET | `/v1/budgets/special/{id}` | è·åä¸é¡¹é¢ç®è¯¦æ |
| GET | `/v1/budgets/special/available` | è·åå¯ç¨ä¸é¡¹é¢ç® |
| POST | `/v1/budgets/daily/carryover` | æ§è¡é¢ç®ç»è½¬ |
| POST | `/v1/budgets/special` | åå»ºä¸é¡¹é¢ç® |
| POST | `/v1/budgets/special/{id}/complete` | ç»æä¸é¡¹é¢ç® |
| PUT | `/v1/budgets/daily` | è®¾ç½®æ¥å¸¸é¢ç® |
| PUT | `/v1/budgets/special/{id}` | æ´æ°ä¸é¡¹é¢ç® |

## è®¤è¯ç®¡ç

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/v1/auth/dev-login` | å¼åç¯å¢ç»å½ |
| POST | `/v1/auth/login` | å¾®ä¿¡ç»å½ |
| POST | `/v1/auth/logout` | éåºç»å½ |
| POST | `/v1/auth/refresh-token` | å·æ°Token |

