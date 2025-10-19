package com.yangjiayu.exam_system_server_online.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangjiayu.exam_system_server_online.entity.Notice;
import com.yangjiayu.exam_system_server_online.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yangjiayu.exam_system_server_online.service.NoticeService;
import org.springframework.stereotype.Service;

/**
 * 公告表服务接口实现
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
@Slf4j
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

}