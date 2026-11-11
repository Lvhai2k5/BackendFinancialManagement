package ute.fit.financemanagement.service;

import java.util.Map;

/** Bọc lời gọi LLM để đổi nhà cung cấp chỉ phải thay 1 lớp cài đặt. */
public interface GeminiClient {

    /**
     * Gửi 1 yêu cầu và nhận về chuỗi JSON đúng schema đã cho.
     *
     * @param systemPrompt chỉ dẫn hệ thống (quy tắc trích xuất, ngày hiện tại, danh mục)
     * @param userText     nội dung người dùng (transcript)
     * @param jsonSchema   schema (tập con OpenAPI của Gemini) ép định dạng đầu ra
     */
    String generateJson(String systemPrompt, String userText, Map<String, Object> jsonSchema);
}
