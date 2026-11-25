package ute.fit.financemanagement.service;

import ute.fit.financemanagement.dto.request.VisionRequests;
import ute.fit.financemanagement.dto.response.VoiceResponses;

public interface VisionScanService {

    /** Ảnh đồ vật → gợi ý điền form (tên vật phẩm, danh mục); số tiền luôn để người dùng nhập. */
    VoiceResponses.Parsed scanItem(VisionRequests.ScanItem request, Long personId);
}
