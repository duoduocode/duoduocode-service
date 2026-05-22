package com.duoduocode.service.transaction.mapper;

import com.duoduocode.service.transaction.dto.TransactionSearchQuery;
import com.duoduocode.service.transaction.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 交易 Mapper 接口
 */
@Mapper
public interface TransactionMapper {

    /**
     * 插入交易
     */
    int insert(Transaction transaction);

    /**
     * 根据ID更新交易
     */
    int updateById(Transaction transaction);

    /**
     * 根据ID查询交易
     */
    Transaction selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询交易列表（分页）
     */
    List<Transaction> selectByUserId(@Param("userId") Long userId,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate);

    /**
     * 根据用户ID统计交易总数
     */
    Long countByUserId(@Param("userId") Long userId,
                       @Param("startDate") String startDate,
                       @Param("endDate") String endDate);

    /**
     * 获取最近交易列表
     */
    List<Transaction> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 软删除交易
     */
    int softDeleteById(@Param("id") Long id);

    /**
     * 检查重复交易
     */
    Transaction checkDuplicate(@Param("userId") Long userId,
                               @Param("amount") String amount,
                               @Param("date") String date,
                               @Param("description") String description);

    /**
     * 搜索交易
     */
    List<Transaction> search(@Param("userId") Long userId,
                             @Param("keyword") String keyword,
                             @Param("accountId") Long accountId,
                             @Param("minAmount") String minAmount,
                             @Param("maxAmount") String maxAmount,
                             @Param("startDate") String startDate,
                             @Param("endDate") String endDate,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);

    /**
     * 搜索交易总数
     */
    Long countSearch(@Param("userId") Long userId,
                     @Param("keyword") String keyword,
                     @Param("accountId") Long accountId,
                     @Param("minAmount") String minAmount,
                     @Param("maxAmount") String maxAmount,
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate);

    /**
     * 更新退款状态
     */
    int updateRefundStatus(@Param("id") Long id,
                           @Param("refundStatus") String refundStatus,
                           @Param("refundedAmount") String refundedAmount);

    /**
     * 高级搜索交易（支持多条件组合筛选）
     */
    List<Transaction> searchTransactions(@Param("userId") Long userId,
                                         @Param("query") TransactionSearchQuery query);

    /**
     * 高级搜索交易总数
     */
    Long countSearchTransactions(@Param("userId") Long userId,
                                 @Param("query") TransactionSearchQuery query);

    /**
     * 根据分类ID统计交易数量
     */
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 批量更新交易的分类ID（分类迁移）
     */
    int updateCategoryId(@Param("oldCategoryId") Long oldCategoryId,
                         @Param("newCategoryId") Long newCategoryId);

    /**
     * 根据账户ID查询交易列表（分页）
     */
    List<Transaction> selectByAccountId(@Param("accountId") Long accountId,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    /**
     * 根据账户ID统计交易总数
     */
    Long countByAccountId(@Param("accountId") Long accountId);

    /**
     * 获取账户最近交易列表
     */
    List<Transaction> selectRecentByAccountId(@Param("accountId") Long accountId,
                                               @Param("limit") Integer limit);
}
