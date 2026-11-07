package ute.fit.financemanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ute.fit.financemanagement.enums.AccountRole;
import ute.fit.financemanagement.exception.ApiException;

/** Người dùng đang đăng nhập, lấy từ JWT (sub = personId). */
public record CurrentUser(Long personId, AccountRole role) {

    public static CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser user)) {
            throw ApiException.unauthorized("Chưa đăng nhập hoặc token đã hết hạn");
        }
        return user;
    }
}
