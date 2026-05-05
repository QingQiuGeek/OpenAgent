package com.qingqiu.openagent.agent.tools;

import static com.qingqiu.openagent.constant.Common.FILE_SAVE_DIR;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.io.File;
import org.springframework.stereotype.Component;

/**
 * 资源下载工具
 */
@Component
public class ResourceDownloadTool implements ITool {

    @Override
    public String getDescription() {
        return "Tool for downloading resources from the internet. Provide a URL and a file name to save the resource.";
    }

    @Override
    public String getName() {
        return "ResourceDownloadTool";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool("Download resource from a given URL")
    public String resourceDownloadTool(@P(value = "URL of the resource to download") String url
        , @P(value = "Name of the file to save the downloaded resource") String fileName) {
        String fileDir = FILE_SAVE_DIR + "/download";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 使用 Hutool 的 downloadFile 方法下载资源
            HttpUtil.downloadFile(url, new File(filePath));
            return "Resource downloaded successfully to: " + filePath;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
