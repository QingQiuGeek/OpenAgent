package com.qingqiu.openagent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.response.UploadFileResponse;
import com.qingqiu.openagent.util.OSSUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件上传接口：将客户端上传的文件转存到 OSS，返回公网 URL 与元数据。
 * <p>
 * 前端在创建包含附件的聊天消息前调用此接口，把得到的 URL/元数据
 * 写入 {@code message.metadata.attachments}，跟消息一起入库。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    /** 聊天附件统一放在 OSS 的 chat 子目录下 */
    private static final String CHAT_DIR = "chat";

    @SaCheckLogin
    @PostMapping("/upload")
    public R<UploadFileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(),
                    "上传文件不能为空");
        }
        String url = OSSUtil.upload(file, CHAT_DIR);
        return R.success(UploadFileResponse.builder()
                .url(url)
                .name(file.getOriginalFilename())
                .size(file.getSize())
                .contentType(file.getContentType())
                .build());
    }
}
