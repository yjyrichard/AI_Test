package com.yangjiayu.exam_system_server_online.service.impl;

import com.yangjiayu.exam_system_server_online.config.properties.MinioProperties;
import com.yangjiayu.exam_system_server_online.service.FileUploadService;
import io.minio.*;
import io.minio.errors.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * @Classname FileUploadServiceImpl
 * @Description 文件上传实现类
 * @Date 2025/10/17 12:29
 * @Created by YangJiaYu
 */
@Slf4j
@Service
@AllArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private MinioClient minioClient;
    private MinioProperties  minioProperties;


    @Override
    public String uploadFile(String folder, MultipartFile file)
        throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        // 1.连接minio的客户端

        // 2.判断桶是否存在
        boolean bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());

        // 3.不存在才创建桶，并且设置访问权限
        if (!bucketExists) {
            log.info("桶 {} 不存在，开始创建...", minioProperties.getBucketName());
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());

            String config = """
                            {
                                  "Statement" : [ {
                                    "Action" : "s3:GetObject",
                                    "Effect" : "Allow",
                                    "Principal" : "*",
                                    "Resource" : "arn:aws:s3:::%s/*"
                                  } ],
                                  "Version" : "2012-10-17"
                            }
                        """.formatted(minioProperties.getBucketName());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(minioProperties.getBucketName())
                .config(config)
                .build());
            log.info("桶 {} 创建成功并设置访问权限", minioProperties.getBucketName());
        }

    //3. 处理上传的对象名（影响，minio桶中的文件结构！）
    //现在： 桶名 / folder / ai.png  缺点： 所有文件都平铺（banner，video）不好区分！ 核心缺点，可能覆盖！
    //小知识点： x/x/x.png -> exam0625 /x/x/ x.png
    //解决覆盖问题： 确保对象和文件的名字唯一即可！！ uuid - - -
    //1.需要添加文件夹 2.添加uuid确保不重复
        String objectName = folder + "/" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/" +
        UUID.randomUUID().toString().replaceAll("-","")+"_"+ file.getOriginalFilename();

        log.debug("文件上传核心业务方法，处理后的文件对象名：{}",objectName);

    //4. 上传文件 putObject方法
    //putObject . 上传文件数据 .steam(文件输入流)
    //uploadObject .上传文件数据 .filename(文件的磁盘地址 c:\\)
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(minioProperties.getBucketName())
        .contentType(file.getContentType())
        .object(objectName) //对象
            .stream(file.getInputStream(),file.getSize(),-1) //-1 我们不指定文件切割大小！让minio自动处理！
        .build());

        //5. 拼接回显地址 【端点 + 桶 + 对象名】
        String url = String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), objectName);
        log.info("文件上传核心业务，完成{}文件上传，返回地址为：{}",objectName,url);
        return url;
    }
}
