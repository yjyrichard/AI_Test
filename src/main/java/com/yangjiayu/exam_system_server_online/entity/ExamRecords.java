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
 * (exam_records)实体类
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("exam_records")
public class ExamRecords extends Model<ExamRecords> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
	private Integer id;
    /**
     * examId
     */
    private Integer examId;
    /**
     * studentName
     */
    private String studentName;
    /**
     * score
     */
    private Integer score;
    /**
     * answers
     */
    private String answers;
    /**
     * startTime
     */
    private Date startTime;
    /**
     * endTime
     */
    private Date endTime;
    /**
     * status
     */
    private String status;
    /**
     * windowSwitches
     */
    private Integer windowSwitches;
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