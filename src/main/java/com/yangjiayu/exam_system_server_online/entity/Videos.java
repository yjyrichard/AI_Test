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
 * 视频信息表(videos)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("videos")
public class Videos extends Model<Videos> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频ID
     */
    @TableId
	private Long id;
    /**
     * 视频标题
     */
    private String title;
    /**
     * 视频描述
     */
    private String description;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 视频文件URL
     */
    private String fileUrl;
    /**
     * 封面图片URL
     */
    private String coverUrl;
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 上传者名称
     */
    private String uploaderName;
    /**
     * 上传者类型：1-用户投稿，2-管理员上传
     */
    private Integer uploaderType;
    /**
     * 上传用户ID（用户投稿时）
     */
    private Long userId;
    /**
     * 管理员ID（管理员上传时）
     */
    private Long adminId;
    /**
     * 状态：0-待审核，1-已发布，2-已拒绝，3-已下架
     */
    private Integer status;
    /**
     * 审核管理员ID
     */
    private Long auditAdminId;
    /**
     * 审核时间
     */
    private Date auditTime;
    /**
     * 审核原因（拒绝时）
     */
    private String auditReason;
    /**
     * 观看次数
     */
    private Long viewCount;
    /**
     * 点赞次数
     */
    private Long likeCount;
    /**
     * 标签，逗号分隔
     */
    private String tags;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 更新时间
     */
    private Date updatedAt;

}