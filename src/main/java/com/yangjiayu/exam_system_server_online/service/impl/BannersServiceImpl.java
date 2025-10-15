package com.yangjiayu.exam_system_server_online.service.impl;

import com.yangjiayu.exam_system_server_online.mapper.BannersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yangjiayu.exam_system_server_online.service.BannersService;
import org.springframework.stereotype.Service;

/**
 * 轮播图表服务接口实现
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description 
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class BannersServiceImpl implements BannersService {
    private final BannersMapper bannersMapper;

}