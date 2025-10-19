package com.yangjiayu.exam_system_server_online.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {

    //    @JsonProperty("hahaha")
    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    //@JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss" ,timezone = "GMT+8")
//    @JsonIgnore
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    //@JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss" ,timezone = "GMT+8")
    @JsonIgnore
    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @Schema(description = "逻辑删除")
    @TableField("is_deleted")
    @TableLogic
    @JsonIgnore
    private Byte isDeleted;


}

/*
 ✅ 是的,足够了! 因为:
  1. @TableLogic 注解已经在字段上,会自动识别
  2. 默认值就是 0=未删除, 1=已删除

  如果你想自定义删除标识值,可以这样:

  @TableLogic(value = "0", delval = "1")  // value=未删除值, delval=已删除值
  private Byte isDeleted;

  或者在全局配置:

  mybatis-plus:
    global-config:
      db-config:
        logic-delete-field: isDeleted
        logic-delete-value: 1      # 删除后的值
        logic-not-delete-value: 0  # 未删除的值

  总结:
  - 当前配置已经生效,0=未删除, 1=已删除 ✅
  - 调用 mapper.deleteById(id) 时会自动执行 UPDATE ... SET is_deleted = 1 而不是真正删除
  - 查询时会自动添加 WHERE is_deleted = 0 条件
 */