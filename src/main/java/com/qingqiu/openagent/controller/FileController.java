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

import java.util.Set;

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

    /** 单文件大小上限：5MB（与前端 ACCEPT_EXTS、spring.servlet.multipart.max-file-size 三者对齐） */
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    /**
     * 聊天附件扩展名白名单（小写，不含点）。
     * <p>完全禁止视频 / 音频 / 可执行文件。以后加格式只需在这里添加。
     */
    private static final Set<String> ALLOWED_EXTS = Set.of(
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            // 纯文本 / 标记
            "txt", "md", "rtf", "csv", "html", "xml", "json", "yaml", "yml",
            // 代码
            "java", "js", "jsx", "ts", "tsx", "py", "go", "rs",
            "cpp", "c", "h", "cs", "rb", "php", "sql",
            // 图片
            "png", "jpg", "jpeg", "gif", "webp"
    );

    @SaCheckLogin
    @PostMapping("/upload")
    public R<UploadFileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(),
                    "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(),
                    "单文件不能超过 5MB");
        }
        String original = file.getOriginalFilename();
        String ext = extractExt(original);
        if (ext == null || !ALLOWED_EXTS.contains(ext)) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(),
                    "不支持的文件类型：" + (ext == null ? original : ext));
        }
        String url = OSSUtil.upload(file, CHAT_DIR);
        return R.success(UploadFileResponse.builder()
                .url(url)
                .name(original)
                .size(file.getSize())
                .contentType(file.getContentType())
                .build());
    }

    /** 取文件名最后一个 {@code .} 后的小写扩展名；无扩展名返回 null。 */
    private static String extractExt(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return null;
        return filename.substring(dot + 1).toLowerCase();
    }
}
