package com.yangjiayu.exam_system_server_online.service.impl;

import com.yangjiayu.exam_system_server_online.mapper.VideoCategoriesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yangjiayu.exam_system_server_online.service.VideoCategoriesService;
import org.springframework.stereotype.Service;

/**
 * 视频分类表服务接口实现
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VideoCategoriesServiceImpl implements VideoCategoriesService {
    private final VideoCategoriesMapper videoCategoriesMapper;

}