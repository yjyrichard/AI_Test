package com.yangjiayu.exam_system_server_online.mapper;

import com.yangjiayu.exam_system_server_online.entity.VideoLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频点赞表(video_likes)数据Mapper
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
*/
@Mapper
public interface VideoLikeMapper extends BaseMapper<VideoLike> {

}
