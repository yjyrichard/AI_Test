package com.yangjiayu.exam_system_server_online.service.impl;

import com.baomidou.mybatisplus.core.injector.methods.SelectOne;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yangjiayu.exam_system_server_online.entity.Banner;
import com.yangjiayu.exam_system_server_online.mapper.BannerMapper;
import com.yangjiayu.exam_system_server_online.service.FileUploadService;
import io.minio.errors.*;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yangjiayu.exam_system_server_online.service.BannerService;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 轮播图表服务接口实现
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
@Slf4j
@Service
@AllArgsConstructor
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {
    private FileUploadService fileUploadService;

    @Override
    public String uploadBannerImage(MultipartFile file)
        throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //文件非空校验
        if(file.isEmpty()){
            throw new RuntimeException("上传的轮播图文件为空！上传失败！");
        }
        //文件类型 image -> memetype image / png gif jpeg
        String contentType = file.getContentType();
        if(ObjectUtils.isEmpty(contentType) || !contentType.startsWith("image")){
            throw new RuntimeException("上传的轮播图文件类型错误！上传失败！");
        }
        // 调用上传业务
        String imageUrl = fileUploadService.uploadFile("banners", file);

        return imageUrl;
    }

    /**
     * 添加轮播图
     * @param banner
     */
    @Override
    public void addBanner(Banner banner) {
        //1.确认banner createTime和updateTime有时间
        //方式1：数据库设置时间  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
        //方案2：代码时间赋值   set new Date();
        //方案3：使用mybatis-plus自动填充功能 [知识点中会说明]
        //2.判断下启动状态
        if (banner.getIsActive() == null){
            banner.setIsActive(true);
        }
        //3.判断优先级
        if (banner.getSortOrder() == null){
            banner.setSortOrder(0);
        }
        //4.进行保存
        boolean isSuccess = save(banner);

        if (!isSuccess) {
            throw new RuntimeException("轮播图保存失败！");
        }

        log.info("轮播图保存成功！！");
    }


    /**
     * 轮播图更新业务！
     * 更新错误，抛出异常
     * @param banner
     */
    @Override
    public void updateBanner(Banner banner) {
        boolean success = this.updateById(banner);
        if (!success) {
            throw new RuntimeException("轮播图更新失败");
        }
    }
}
