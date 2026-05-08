package com.qingqiu.openagent.util;

import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Set;

/**
 * 附件内容提取器：从 OSS URL 下载文件，用 Apache Tika 解析出纯文本，供 LLM 理解文件内容。
 *
 * <p>安全策略：
 * <ul>
 *   <li>仅允许 HTTPS 协议（防止 SSRF 明文探测）</li>
 *   <li>下载上限 {@value MAX_DOWNLOAD_BYTES} 字节，防止超大文件耗尽内存</li>
 *   <li>提取文本上限 {@value MAX_TEXT_CHARS} 字符，防止 LLM context 溢出</li>
 *   <li>拒绝解析可执行/二进制类型（黑名单 MIME）</li>
 *   <li>图片 / 视频 / 音频跳过（Tika 无法提取有效文本，直接标注为"图片附件"）</li>
 *   <li>Tika 本身只做文本提取，不执行任何嵌入脚本或宏</li>
 * </ul>
 */
@Component
public class AttachmentContentExtractor {

    private static final Logger log = LoggerFactory.getLogger(AttachmentContentExtractor.class);

    /** 单文件下载字节上限：10 MB */
    static final long MAX_DOWNLOAD_BYTES = 10L * 1024 * 1024;

    /**
     * 提取文本字符上限：50,000 字符（≈ 35,000 token）。
     * 超出部分 Tika 抛 {@link WriteLimitReachedException}，已提取内容依然保留。
     */
    static final int MAX_TEXT_CHARS = 50_000;

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS    = 30_000;

    /**
     * 可执行 / 无意义二进制 MIME 黑名单前缀，拒绝解析。
     * Tika 本身不执行代码，但这类文件即使解析也得不到有用文本，且可能触发解析异常。
     */
    private static final Set<String> DENIED_MIME_PREFIXES = Set.of(
            "application/x-executable",
            "application/x-sharedlib",
            "application/x-msdownload",
            "application/x-msdos-program",
            "application/x-dex",
            "application/x-sh",
            "application/x-csh",
            "application/x-bat"
    );

    /**
     * 图片 / 视频 / 音频：无可提取文本，跳过解析直接返回 null。
     */
    private static final Set<String> SKIP_MIME_PREFIXES = Set.of(
            "image/", "video/", "audio/"
    );

    /**
     * 从 URL 下载文件并提取纯文本。
     *
     * @param fileUrl     文件公网 URL（须以 {@code https://} 开头）
     * @param contentType 前端上报的 MIME（可为 null），仅用于快速跳过图片等
     * @return 提取的文本（可能已截断）；无法提取或为图片时返回 null
     */
    public String extract(String fileUrl, String contentType) {
        if (!StringUtils.hasLength(fileUrl)) return null;

        // 仅允许 HTTPS，防止 SSRF 访问内网明文服务
        if (!fileUrl.startsWith("https://")) {
            log.warn("[Tika] 拒绝非 HTTPS URL: {}", fileUrl);
            return null;
        }

        // 根据前端上报的 contentType 快速跳过图片/视频/音频
        if (contentType != null && shouldSkip(contentType)) {
            return null;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(fileUrl).toURL().openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "OpenAgent-TikaExtractor/1.0");

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                log.warn("[Tika] HTTP {} 下载失败: {}", status, fileUrl);
                return null;
            }

            // 以服务端返回的 Content-Type 为准做二次过滤
            String serverCt = conn.getContentType();
            if (serverCt != null) {
                String mime = serverCt.split(";")[0].trim().toLowerCase();
                if (isDenied(mime)) {
                    log.warn("[Tika] 拒绝解析类型 {}: {}", mime, fileUrl);
                    return null;
                }
                if (shouldSkip(mime)) {
                    return null;
                }
            }

            try (InputStream raw = conn.getInputStream();
                 LimitedInputStream limited = new LimitedInputStream(raw, MAX_DOWNLOAD_BYTES)) {

                AutoDetectParser parser  = new AutoDetectParser();
                BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_CHARS);
                Metadata metadata        = new Metadata();
                if (StringUtils.hasLength(contentType)) {
                    // Tika 2.x: 用 TikaCoreProperties.CONTENT_TYPE_USER_OVERRIDE 提示类型
                    metadata.set(TikaCoreProperties.CONTENT_TYPE_USER_OVERRIDE, contentType);
                }

                try {
                    parser.parse(limited, handler, metadata, new ParseContext());
                } catch (WriteLimitReachedException e) {
                    // 超出字符上限：保留已提取部分，只记录 info 日志
                    log.info("[Tika] 内容超 {} 字已截断: {}", MAX_TEXT_CHARS, fileUrl);
                }

                // Tika 自动检测后的真实 MIME 做最终校验
                String detected = metadata.get("Content-Type");
                if (detected == null) {
                    detected = metadata.get(TikaCoreProperties.CONTENT_TYPE_USER_OVERRIDE);
                }
                if (detected != null) {
                    String mime = detected.split(";")[0].trim().toLowerCase();
                    if (isDenied(mime)) {
                        log.warn("[Tika] 检测为拒绝类型 {} url={}", mime, fileUrl);
                        return null;
                    }
                }

                String text = handler.toString().strip();
                return text.isEmpty() ? null : text;
            }
        } catch (Exception e) {
            log.warn("[Tika] 提取失败 url={} err={}", fileUrl, e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean isDenied(String mime) {
        return DENIED_MIME_PREFIXES.stream().anyMatch(mime::startsWith);
    }

    private boolean shouldSkip(String mime) {
        String lower = mime.toLowerCase();
        return SKIP_MIME_PREFIXES.stream().anyMatch(lower::startsWith);
    }

    /**
     * 字节数限制流：读取超过 {@code limit} 字节后返回 EOF，防止超大文件撑爆内存。
     */
    private static class LimitedInputStream extends FilterInputStream {
        private long remaining;

        LimitedInputStream(InputStream in, long limit) {
            super(in);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = super.read();
            if (b >= 0) remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int n = super.read(b, off, toRead);
            if (n > 0) remaining -= n;
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            long toSkip = Math.min(n, remaining);
            long skipped = super.skip(toSkip);
            remaining -= skipped;
            return skipped;
        }
    }
}
