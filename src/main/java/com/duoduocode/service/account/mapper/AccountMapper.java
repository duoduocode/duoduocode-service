package com.duoduocode.service.account.mapper;

import com.duoduocode.service.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户 Mapper 接口
 */
@Mapper
public interface AccountMapper {

    /**
     * 插入账户
     */
    int insert(Account account);

    /**
     * 根据ID更新账户
     */
    int updateById(Account account);

    /**
     * 根据ID查询账户
     */
    Account selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询账户列表
     */
    List<Account> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和类型查询账户列�?     */
    List<Account> selectByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    /**
     * 检查账户名称是否存�?     */
    int countByUserIdAndName(@Param("userId") Long userId, @Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 计算账户当前余额
     * 当前余额 = 初始余额 + SUM(借方) - SUM(贷方)
     */
    BigDecimal calculateCurrentBalance(@Param("accountId") Long accountId);

    /**
     * 软删除账�?     */
    int softDeleteById(@Param("id") Long id);
}
