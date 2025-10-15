package com.yangjiayu.exam_system_server_online.controller;

import com.yangjiayu.exam_system_server_online.service.VideoViewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 视频观看记录表服务控制器
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/videoViews")
public class VideoViewsController {
    private final VideoViewsService videoViewsService;

}