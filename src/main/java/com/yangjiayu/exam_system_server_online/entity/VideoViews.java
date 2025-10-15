package com.yangjiayu.exam_system_server_online.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 视频观看记录表(video_views)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("video_views")
public class VideoViews extends Model<VideoViews> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 观看ID
     */
    @TableId
	private Long id;
    /**
     * 视频ID
     */
    private Long videoId;
    /**
     * 用户IP
     */
    private String userIp;
    /**
     * 用户代理信息
     */
    private String userAgent;
    /**
     * 观看时长（秒）
     */
    private Integer viewDuration;
    /**
     * 观看时间
     */
    private Date createdAt;

}