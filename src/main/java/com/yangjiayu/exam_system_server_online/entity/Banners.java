package com.yangjiayu.exam_system_server_online.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 轮播图表(banners)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("banners")
public class Banners extends Model<Banners> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 轮播图ID
     */
    @TableId
	private Long id;
    /**
     * 轮播图标题
     */
    private String title;
    /**
     * 轮播图描述
     */
    private String description;
    /**
     * 图片URL
     */
    private String imageUrl;
    /**
     * 跳转链接
     */
    private String linkUrl;
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    /**
     * 是否启用
     */
    private Integer isActive;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    @TableField(update = "now()")
	private Date updateTime;
    /**
     * 0-未删除，1-已删除
     */
    private Integer isDeleted;

}