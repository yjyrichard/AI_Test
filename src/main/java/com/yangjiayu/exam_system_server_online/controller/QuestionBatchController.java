package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.utils.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * @Classname QuestionBatchController
 * @Description 题目批量处理
 * @Date 2025/10/19 21:00
 * @Created by YangJiaYu
 */
@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("questions/batch")
@Tag(name = "题目批量操作",description = "题目批量操作管理相关操作，包括Excel,AI生成题目,批量验证等功能")
public class QuestionBatchController {

    /**
     * 下载Excel导入模板
     * @return Excel模板文件
     */
    @GetMapping("/template")  // 处理GET请求
    @Operation(summary = "下载Excel导入模板", description = "下载题目批量导入的Excel模板文件")  // API描述
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        //1.获取下载模板字节数组
        byte[] template = ExcelUtil.generateTemplate();
        //2.封装ResponseEntity
        ResponseEntity<byte[]> responseEntity = ResponseEntity.ok()
            .header("Content-Disposition","attachment;filename=question_import_template.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM) //二进制文件，不确定类型！！
            .body(template);
        return responseEntity;
    }


}
