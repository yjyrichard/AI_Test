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
 * 公告表(notices)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("notices")
public class Notices extends Model<Notices> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 公告ID
     */
    @TableId
	private Long id;
    /**
     * 公告标题
     */
    private String title;
    /**
     * 公告内容
     */
    private String content;
    /**
     * 公告类型：SYSTEM(系统)、FEATURE(新功能)、NOTICE(通知)
     */
    private String type;
    /**
     * 优先级：0-普通，1-重要，2-紧急
     */
    private Integer priority;
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