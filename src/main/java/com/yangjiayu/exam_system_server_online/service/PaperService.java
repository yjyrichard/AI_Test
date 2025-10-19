package com.yangjiayu.exam_system_server_online.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yangjiayu.exam_system_server_online.entity.Paper;
import com.yangjiayu.exam_system_server_online.vo.AiPaperVo;
import com.yangjiayu.exam_system_server_online.vo.PaperVo;

/**
 * 服务接口
 *
 * @author yangjiayu
 * @since 2025-10-15 21:05:46
 * @description
 */
public interface PaperService extends IService<Paper> {

    Paper customPaperDetailById(Long id);

    Paper customCreatePaper(PaperVo paperVo);

    Paper customAiCreatePaper(AiPaperVo aiPaperVo);

    Paper customUpdatePaper(Integer id, PaperVo paperVo);

    void customUpdatePaperStatus(Integer id, String status);

    void customRemoveId(Integer id);
}
