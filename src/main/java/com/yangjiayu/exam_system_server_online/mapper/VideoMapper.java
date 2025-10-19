package com.yangjiayu.exam_system_server_online.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yangjiayu.exam_system_server_online.entity.Video;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频信息表(videos)数据Mapper
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
*/
@Mapper
public interface VideoMapper extends BaseMapper<Video> {

}
