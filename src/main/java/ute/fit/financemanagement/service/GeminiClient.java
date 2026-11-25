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

    /**
     * Như {@link #generateJson} nhưng đầu vào là 1 ảnh (nhận diện đồ vật).
     *
     * @param mimeType    kiểu ảnh, ví dụ {@code image/jpeg}
     * @param base64Image dữ liệu ảnh đã mã hoá base64 (không kèm tiền tố data:)
     */
    String generateJsonFromImage(String systemPrompt, String mimeType, String base64Image,
                                 Map<String, Object> jsonSchema);
}
