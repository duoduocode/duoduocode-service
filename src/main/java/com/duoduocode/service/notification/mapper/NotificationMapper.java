package com.duoduocode.service.notification.mapper;

import com.duoduocode.service.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 站内消息/待办 Mapper 接口
 */
@Mapper
public interface NotificationMapper {

    /**
     * 幂等插入（ON DUPLICATE KEY UPDATE id=id，已存在即忽略）
     *
     * @return 1=新插入，0=已存在被忽略
     */
    int insertIgnore(Notification notification);

    /**
     * 分页查询（待办置顶，组内按创建时间倒序）
     *
     * @param msgType all/todo/notice
     */
    List<Notification> selectPage(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit,
                                  @Param("msgType") String msgType);

    /**
     * 按条件统计总数
     */
    Long countByUserId(@Param("userId") Long userId, @Param("msgType") String msgType);

    /**
     * 按消息形态统计未读数
     *
     * @return [{msgType, cnt}]
     */
    List<Map<String, Object>> countUnreadByMsgType(@Param("userId") Long userId);

    /**
     * 按 ID 批量标记已读（仅通知型生效，待办以完成动作消化）
     */
    int markRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /**
     * 全部已读（仅通知型生效）
     */
    int markAllNoticeRead(@Param("userId") Long userId);

    /**
     * 按幂等键精确消化待办（status → digested），不牵连同模板其他期次
     */
    int digestByDedupKey(@Param("userId") Long userId,
                         @Param("dedupKey") String dedupKey);

    /**
     * 物理删除过期的已读通知
     */
    int deleteExpiredReadNotices(@Param("before") LocalDateTime before);

    /**
     * 物理删除过期的已消化待办
     */
    int deleteExpiredDigestedTodos(@Param("before") LocalDateTime before);
}
