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
 * 视频点赞表(video_likes)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("video_likes")
public class VideoLikes extends Model<VideoLikes> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 点赞ID
     */
    @TableId
	private Long id;
    /**
     * 视频ID
     */
    private Long videoId;
    /**
     * 用户IP（匿名点赞）
     */
    private String userIp;
    /**
     * 用户代理信息
     */
    private String userAgent;
    /**
     * 点赞时间
     */
    private Date createdAt;

}