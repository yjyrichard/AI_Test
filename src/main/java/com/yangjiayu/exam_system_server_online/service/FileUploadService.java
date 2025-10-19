package com.yangjiayu.exam_system_server_online.service;

import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @Classname FileUploadService
 * @Description 文件上传服务 支持MINIO和本地文件存储这两种方式
 * @Date 2025/10/17 12:15
 * @Created by YangJiaYu
 */

public interface FileUploadService {

    /**
     * 文件上传业务方法
     * @param folder 在minio中存储的文件夹 （轮播图：banners,视频：videos）
     * @param file 上传的文件对象
     * @return 返回的回显地址
     */
    String uploadFile(String folder,MultipartFile file)
        throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
}
