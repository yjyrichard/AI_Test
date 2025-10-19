package com.yangjiayu.exam_system_server_online.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yangjiayu.exam_system_server_online.common.Result;
import com.yangjiayu.exam_system_server_online.entity.Banner;
import com.yangjiayu.exam_system_server_online.service.BannerService;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 轮播图表服务控制器 - 处理轮播图相关的HTTP请求
 * 包括图片上传，轮播图的CRUD曹祖，状态切换等功能
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
@Slf4j
@RequiredArgsConstructor
@RestController  // REST 控制器，返回JSON数据
@RequestMapping("/banners") //
//@CrossOrigin //允许跨域访问
@Tag(name = "轮播图管理",description = "轮播图相关操作，包括图片上传，轮播图CRUD，状态管理等功能") //Swagger API分组
public class BannerController {
    private final BannerService bannerService;


    /**
     * 获取所有轮播图
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有轮播图",description = "获取所有轮播图列表，包括启用和禁用，供管理后台使用")//API描述
    public Result<List<Banner>> getAllBanners(){
        //1 .查询所有轮播图集合
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Banner::getSortOrder);
        List<Banner> bannerList = bannerService.list(queryWrapper);

        log.info("获取所有轮播图成功！轮播图数量为：{}，具体数据为：{}",bannerList.size(),bannerList);
        // 返回数据
        return Result.success(bannerList);
    }


    /**
     * 前台需要的轮播图数据接口 查询所有激活条件
     * 逻辑删除 + 忽略字段 + 时间格式 + 时区设置 + 日志输出
     * @return
     */
    @GetMapping("active")
    @Operation(summary = "前台需要的激活轮播图接口")
    public Result<List<Banner>> activeBanners(){
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Banner::getIsActive,true);
        queryWrapper.orderByAsc(Banner::getSortOrder);
        List<Banner> bannerList = bannerService.list(queryWrapper);
        log.info("查询前台激活轮播图接口调用成功！，查询数量为：{}，具体数据为:{}",bannerList.size(),bannerList);
        return Result.success(bannerList);
    }

    @PutMapping("toggle/{id}")
    @Operation(summary = "切换轮播图状态")
    public Result<String> toggleBanner(
        @Parameter(description = "轮播图ID") @PathVariable Long id,
        @Parameter(description = "是否启用，true为启用，false为禁用")@RequestParam Boolean isActive){
        LambdaUpdateWrapper<Banner> lambdaUpdateWrapper = new LambdaUpdateWrapper();
        lambdaUpdateWrapper.eq(Banner::getId,id);
        lambdaUpdateWrapper.set(Banner::getIsActive,isActive);

        bannerService.update(lambdaUpdateWrapper);
        log.info("id={}轮播图，状态修改成功！，修改后的状态为：{}",id,isActive);
        return Result.success("修改成功！");
    }


    // 防御性变成问题：删除会影响数据的展示和数据的丢失问题
    // 1.你是不是做了逻辑删除（不能真删除）
    // 2.有没有重要的关联数据，导致你无法删除
    /**
     * 删除轮播图
     * @param id
     * @return
     */
    @DeleteMapping("delete/{id}")
    @Operation(summary = "删除轮播图")
    public Result<String>  deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id){
        //1.根据ID删除对应的轮播图
        bannerService.removeById(id);
        // ======= 日志输出
        log.info("删除id={}轮播图成功！",id);
        return Result.success("删除成功！");
    }


    // 获取轮播图详情接口
    @GetMapping("{id}")
    @Operation(summary = "根据ID获取轮播图")
    public Result<Banner> getBannerById(
        @Parameter(description = "轮播图ID")@PathVariable Long id){
        //1. 跟还有id查询轮播图对象
        Banner byId = bannerService.getById(id);
        return Result.success(byId);
    }

    @PostMapping("/upload-image")
    @Operation(summary = "上传轮播图图片")
    public Result<String> uploadImage(
        @Parameter(description = "要上传的图片文件，支持jpg,png,gif等格式，大小限制5MB")
        @RequestParam("file")MultipartFile file
    )
        throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String imageUrl = bannerService.uploadBannerImage(file);
        log.info("轮播图上传接口调用成功！图片的回显地址为：{}",imageUrl);
        return Result.success(imageUrl,"图片上传成功");
    }


    /**
     * 实现思路：
     *    确认时间 创建和修改时间赋值
     *    确认激活状态true
     *    确认优先级没有默认 0
     *    进行保存即可
     *    单表动作，直接调用mybatis-plus业务层
     * 添加轮播图git
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PostMapping("/add")  // 处理POST请求
    @Operation(summary = "添加轮播图", description = "创建新的轮播图，需要提供图片URL、标题、跳转链接等信息")  // API描述
    public Result<String> addBanner(@RequestBody Banner banner) {
        bannerService.addBanner(banner);
        return Result.success("添加轮播图成功！");
    }


    /**
     * 更新轮播图
     * @param banner 轮播图对象
     * @return 操作结果
     */
    @PutMapping("/update")  // 处理PUT请求
    @Operation(summary = "更新轮播图", description = "更新轮播图的信息，包括图片、标题、跳转链接、排序等")  // API描述
    public Result<String> updateBanner(@RequestBody Banner banner) {
        bannerService.updateBanner(banner);
        return Result.success("更新轮播图成功！");
    }




}
