package com.yangjiayu.exam_system_server_online.mapper;

import com.yangjiayu.exam_system_server_online.entity.VideoCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频分类表(video_categories)数据Mapper
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
*/
@Mapper
public interface VideoCategoryMapper extends BaseMapper<VideoCategory> {

}
