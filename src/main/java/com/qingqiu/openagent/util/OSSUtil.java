package com.qingqiu.openagent.util;

import static com.qingqiu.openagent.constant.Common.OSS_UPLOAD_PREFIX;
import static com.qingqiu.openagent.enums.BizExceptionEnum.FILE_UPLOAD_ERROR;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云 OSS 工具类。
 * <p>
 * 通过 Spring 注入配置到静态字段，对外提供静态方法，可直接通过
 * {@code OSSUtil.upload(...)} 调用。
 */
@Component
@Slf4j
public class OSSUtil {

    private static String accessKey;
    private static String secretKey;
    private static String bucket;
    private static String endpoint;

    @Value("${oss.accessKey}")
    public void setAccessKey(String accessKey) {
        OSSUtil.accessKey = accessKey;
    }

    @Value("${oss.secretKey}")
    public void setSecretKey(String secretKey) {
        OSSUtil.secretKey = secretKey;
    }

    @Value("${oss.bucket}")
    public void setBucket(String bucket) {
        OSSUtil.bucket = bucket;
    }

    @Value("${oss.url}")
    public void setEndpoint(String endpoint) {
        OSSUtil.endpoint = endpoint;
    }

    /** 禁止实例化 */
    private OSSUtil() {
    }

    /**
     * 上传文件到阿里云 OSS。
     *
     * @param file 待上传文件
     * @param dir  OSS 相对目录（可为空），会追加到 {@link com.qingqiu.openagent.constant.Common#OSS_UPLOAD_PREFIX} 之后
     * @return 完整的公网访问 URL
     */
    public static String upload(MultipartFile file, String dir) {
        Assert.notNull(file, "上传文件对象不能为空");
        if (file.isEmpty()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(),
                BizExceptionEnum.PARAMS_ERROR.getMessage());
        }

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            // 1. 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extName = FileUtil.extName(originalFilename);
            String safeName = FileUtil.mainName(originalFilename);

            String currentPrefix = OSS_UPLOAD_PREFIX;
            if (StrUtil.isNotBlank(dir)) {
                currentPrefix = currentPrefix + StrUtil.SLASH + dir;
            }
            String fileName = currentPrefix + StrUtil.SLASH + System.currentTimeMillis()
                + StrUtil.UNDERLINE + safeName + StrUtil.DOT + extName;
            // 2. 上传到阿里云 OSS
            ossClient.putObject(bucket, fileName, file.getInputStream());
            // 3. 返回完整访问 URL
            return "https://" + bucket + "." + endpoint + "/" + fileName;
        } catch (Exception e) {
            log.error("阿里云OSS文件上传失败", e);
            throw new BizException(FILE_UPLOAD_ERROR.getCode(), FILE_UPLOAD_ERROR.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }

}
