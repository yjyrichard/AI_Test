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
 * (answer_record)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:45
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("answer_record")
public class AnswerRecord extends Model<AnswerRecord> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
	private Integer id;
    /**
     * examRecordId
     */
    private Integer examRecordId;
    /**
     * questionId
     */
    private Integer questionId;
    /**
     * userAnswer
     */
    private String userAnswer;
    /**
     * score
     */
    private Integer score;
    /**
     * isCorrect
     */
    private Integer isCorrect;
    /**
     * aiCorrection
     */
    private String aiCorrection;
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