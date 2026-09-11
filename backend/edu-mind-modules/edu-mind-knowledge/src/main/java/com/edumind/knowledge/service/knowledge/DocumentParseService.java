package com.edumind.knowledge.service.knowledge;

public interface DocumentParseService {

    String extractText(byte[] fileBytes, String fileType, String fileName);
}
