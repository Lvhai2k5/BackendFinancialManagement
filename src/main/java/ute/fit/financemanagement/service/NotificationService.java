package ute.fit.financemanagement.service;

import java.util.List;
import ute.fit.financemanagement.dto.response.NotificationResponse;

public interface NotificationService {

    /** Thông báo suy ra từ dữ liệu giao dịch/hạn mức (chưa lưu bảng riêng). */
    List<NotificationResponse> listMine(Long personId);
}