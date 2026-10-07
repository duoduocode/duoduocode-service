package com.duoduocode.service.billimport.mapper;

import com.duoduocode.service.billimport.ImportBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 账单导入批次 Mapper
 */
@Mapper
public interface ImportBatchMapper {

    int insert(ImportBatch batch);

    int updateCounts(@Param("id") Long id,
                     @Param("importedCount") Integer importedCount,
                     @Param("skippedCount") Integer skippedCount,
                     @Param("ignoredCount") Integer ignoredCount,
                     @Param("createdAccounts") String createdAccounts);
}
