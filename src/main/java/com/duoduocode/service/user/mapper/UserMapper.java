package com.duoduocode.service.user.mapper;

import com.duoduocode.service.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {

    /**
     * 根据 OpenID 查找用户
     *
     * @param openid 微信 OpenID
     * @return 用户信息，不存在返回 null
     */
    User selectByOpenid(@Param("openid") String openid);

    /**
     * 根据 ID 查找用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    User selectById(@Param("id") Long id);

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 影响行数
     */
    int updateById(User user);

    /**
     * 更新用户头像
     *
     * @param id        用户ID
     * @param avatarUrl 头像URL
     * @return 影响行数
     */
    int updateAvatarUrl(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);

    /**
     * 更新用户昵称
     *
     * @param id       用户ID
     * @param nickname 昵称
     * @return 影响行数
     */
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    /**
     * 查询用户统计信息：记账天数、总笔数、净资产
     *
     * @param userId 用户ID
     * @return {totalDays, totalCount, netAsset}
     */
    Map<String, Object> selectStats(@Param("userId") Long userId);
}
