package ute.fit.financemanagement.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "FinNest Finance Management API",
                version = "v1",
                description = "REST API cho xác thực, hồ sơ người dùng, gia đình, giao dịch và quản trị."),
        tags = {
                @Tag(name = "Authentication", description = "Đăng nhập, đăng ký, OTP và quản lý mật khẩu."),
                @Tag(name = "Users", description = "Hồ sơ cá nhân và yêu cầu hỗ trợ."),
                @Tag(name = "Families", description = "Gia đình, thành viên, quyền và hạng mục tài chính."),
                @Tag(name = "Transactions", description = "Giao dịch, phê duyệt và từ chối."),
                @Tag(name = "Notifications", description = "Thông báo tài chính của người dùng."),
                @Tag(name = "Admin", description = "Dashboard, tài khoản, ticket và nhật ký quản trị.")
        })
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Dán access token JWT vào Swagger Authorize.")
public class OpenApiConfig {
}
