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
 * (question_choices)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("question_choices")
public class QuestionChoices extends Model<QuestionChoices> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
	private Long id;
    /**
     * questionId
     */
    private Long questionId;
    /**
     * content
     */
    private String content;
    /**
     * isCorrect
     */
    private Integer isCorrect;
    /**
     * sort
     */
    private Integer sort;
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