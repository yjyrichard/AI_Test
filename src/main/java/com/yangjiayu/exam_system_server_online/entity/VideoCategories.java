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
 * 视频分类表(video_categories)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("video_categories")
public class VideoCategories extends Model<VideoCategories> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @TableId
	private Long id;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 分类描述
     */
    private String description;
    /**
     * 父级分类ID，0为顶级
     */
    private Long parentId;
    /**
     * 排序权重
     */
    private Integer sortOrder;
    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 更新时间
     */
    private Date updatedAt;

}