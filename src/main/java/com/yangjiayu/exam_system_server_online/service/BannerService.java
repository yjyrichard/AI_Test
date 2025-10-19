package com.yangjiayu.exam_system_server_online.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangjiayu.exam_system_server_online.entity.Banner;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 轮播图表服务接口
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
public interface BannerService extends IService<Banner> {

    /**
     * 完成轮播图上传
     * @param file
     * @return
     */
    String uploadBannerImage(MultipartFile file)
        throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    /**
     * 添加轮播图
     * @param banner
     */
    void addBanner(Banner banner);

    /**
     * 更新轮播图
     * @param banner
     */
    void updateBanner(Banner banner);
}
