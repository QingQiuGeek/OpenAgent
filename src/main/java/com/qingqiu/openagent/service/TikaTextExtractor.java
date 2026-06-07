package com.qingqiu.openagent.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author: qingqiugeek
 * @description: 用 Apache Tika 从任意文档（pdf/docx/xlsx/代码/纯文本…）抽取纯文本。
 *               对二进制/图片返回空串。
 */
@Service
public class TikaTextExtractor {
//TODO: 1. Tika 默认 100KB 字符上限，调大避免长文档被截断。
// chunk 元数据里记录原文长度，超过上限时提示用户上传更小的文档或使用分块上传。
    /** Tika 默认 100KB 字符上限，调大避免长文档被截断。 */
    private static final int MAX_CHARS = 5_000_000;

    private final Tika tika;

    public TikaTextExtractor() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(MAX_CHARS);
    }

    public String extract(InputStream is) throws IOException, TikaException {
        if (is == null) return "";
        String text = tika.parseToString(is);
        return text == null ? "" : text;
    }
}
