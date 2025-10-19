package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 公告表服务控制器
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/notices")
public class NoticeController  {
    private final NoticeService noticeService;

}