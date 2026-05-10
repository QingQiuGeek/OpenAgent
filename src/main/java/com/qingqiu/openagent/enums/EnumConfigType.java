package com.qingqiu.openagent.enums;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 09:30
 * @description: enum_config.type 的语义常量；DB 才是权威来源，这里只是给 Java 代码用的字符串常量集合
 */
public final class EnumConfigType {

    /** 模型厂商类型。值与 {@code DynamicChatModelService} 的 provider_type 比较保持一致。 */
    public static final String MODEL_PROVIDER_TYPE = "model_provider_type";

    /** 工具类型：FIXED / OPTIONAL。 */
    public static final String TOOL_TYPE = "tool_type";

    /** 知识库文档类型（可被向量化检索）：pdf / docx / md / txt / 代码等，不含图片。 */
    public static final String DOCUMENT_FILETYPE = "document_filetype";

    /** 聊天框附件可上传类型：document_filetype 全集 + 图片。 */
    public static final String UPLOAD_FILETYPE = "upload_filetype";

    /** MCP 传输协议：stdio / sse / http。 */
    public static final String MCP_TRANSPORT = "mcp_transport";

    private EnumConfigType() {}
}
