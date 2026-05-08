package com.qingqiu.openagent.util;

import static com.qingqiu.openagent.constant.Common.OSS_UPLOAD_PREFIX;
import static com.qingqiu.openagent.enums.BizExceptionEnum.FILE_UPLOAD_ERROR;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import java.io.ByteArrayInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 14:30
 * @description: OSS utility class
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

    /**
     * 把远端 URL 指向的资源（如第三方生图服务返回的、带签名 / 有有效期的图片链接）
     * 下载下来转存到自己的 OSS，返回长期可用的 OSS URL。
     *
     * @param upstreamUrl 远端资源 URL（http / https）
     * @param dir         OSS 子目录（相对 {@link com.qingqiu.openagent.constant.Common#OSS_UPLOAD_PREFIX}），可空
     * @return 永久 OSS URL
     */
    public static String uploadFromUrl(String upstreamUrl, String dir) {
        Assert.hasText(upstreamUrl, "upstreamUrl 不能为空");

        // 1. 下载远端字节
        byte[] bytes;
        String contentType;
        try (HttpResponse resp = HttpRequest.get(upstreamUrl)
                .timeout(60_000)
                .header("Referer", "")
                .execute()) {
            if (!resp.isOk()) {
                log.error("下载远端资源失败 status={} url={}", resp.getStatus(), upstreamUrl);
                throw new BizException(FILE_UPLOAD_ERROR.getCode(),
                        "下载远端资源失败：HTTP " + resp.getStatus());
            }
            bytes = resp.bodyBytes();
            contentType = resp.header("Content-Type");
        } catch (BizException be) {
            throw be;
        } catch (Exception e) {
            log.error("下载远端资源异常 url={}", upstreamUrl, e);
            throw new BizException(FILE_UPLOAD_ERROR.getCode(),
                    "下载远端资源异常：" + e.getMessage());
        }
        if (bytes == null || bytes.length == 0) {
            throw new BizException(FILE_UPLOAD_ERROR.getCode(), "远端资源为空");
        }

        // 2. 推断扩展名（优先 Content-Type，再回退到 URL 后缀，最后默认 png）
        String ext = guessExt(contentType, upstreamUrl);

        // 3. 拼接 OSS Key
        String prefix = OSS_UPLOAD_PREFIX;
        if (StrUtil.isNotBlank(dir)) {
            prefix = prefix + StrUtil.SLASH + dir;
        }
        String key = prefix + StrUtil.SLASH
                + System.currentTimeMillis() + StrUtil.UNDERLINE + IdUtil.fastSimpleUUID().substring(0, 8)
                + StrUtil.DOT + ext;

        // 4. 上传
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            ObjectMetadata meta = new ObjectMetadata();
            if (StrUtil.isNotBlank(contentType)) {
                meta.setContentType(contentType);
            }
            meta.setContentLength(bytes.length);
            ossClient.putObject(bucket, key, new ByteArrayInputStream(bytes), meta);
            String ossUrl = "https://" + bucket + "." + endpoint + "/" + key;
            log.info("[OSSUtil] uploadFromUrl ok upstream={} oss={}", upstreamUrl, ossUrl);
            return ossUrl;
        } catch (Exception e) {
            log.error("OSS 上传失败 key={}", key, e);
            throw new BizException(FILE_UPLOAD_ERROR.getCode(), FILE_UPLOAD_ERROR.getMessage());
        } finally {
            ossClient.shutdown();
        }
    }

    /** 根据 Content-Type 或原始 URL 推断扩展名（不含点）。 */
    private static String guessExt(String contentType, String url) {
        if (StrUtil.isNotBlank(contentType)) {
            String ct = contentType.toLowerCase();
            if (ct.contains("png")) return "png";
            if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
            if (ct.contains("webp")) return "webp";
            if (ct.contains("gif")) return "gif";
            if (ct.contains("bmp")) return "bmp";
        }
        if (url != null) {
            String pathOnly = url.split("\\?")[0];
            String e = FileUtil.extName(pathOnly);
            if (StrUtil.isNotBlank(e) && e.length() <= 5) {
                return e.toLowerCase();
            }
        }
        return "png";
    }

}
