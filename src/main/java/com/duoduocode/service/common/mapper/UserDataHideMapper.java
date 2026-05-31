package com.duoduocode.service.common.mapper;

import com.duoduocode.service.common.entity.UserDataHide;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDataHideMapper {

    int insert(UserDataHide record);

    UserDataHide selectByUserAndRef(@Param("userId") Long userId, @Param("dataType") String dataType, @Param("refId") Long refId);
}
