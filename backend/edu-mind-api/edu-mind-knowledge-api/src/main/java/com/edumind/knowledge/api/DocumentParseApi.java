package com.edumind.knowledge.api;

public interface DocumentParseApi {

    /**
     * 从上传的文件字节数组中提取纯文本正文
     *
     * @param fileBytes 文件字节数组
     * @param fileType  文件 MIME 类型
     * @param fileName  原始文件名
     * @return 提取出的纯文本内容
     */
    String extractText(byte[] fileBytes, String fileType, String fileName);
}
