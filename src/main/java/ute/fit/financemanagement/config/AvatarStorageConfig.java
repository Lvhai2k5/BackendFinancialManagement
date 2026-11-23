package ute.fit.financemanagement.config;

import java.util.Map;
import ute.fit.financemanagement.service.AvatarStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
public class AvatarStorageConfig {
    @Bean
    @ConditionalOnProperty(name = "app.avatar.storage-enabled", havingValue = "true")
    Cloudinary cloudinary(@Value("${app.avatar.cloudinary-url}") String cloudinaryUrl) {
        return new Cloudinary(cloudinaryUrl);
    }

    @Bean
    @ConditionalOnProperty(name = "app.avatar.storage-enabled", havingValue = "true")
    AvatarStorage cloudinaryAvatarStorage(Cloudinary cloudinary) {
        return new CloudinaryAvatarStorage(cloudinary);
    }

    @Bean
    @ConditionalOnProperty(name = "app.avatar.storage-enabled", havingValue = "false", matchIfMissing = true)
    AvatarStorage disabledAvatarStorage() {
        return new DisabledAvatarStorage();
    }

    static final class DisabledAvatarStorage implements AvatarStorage {
        @Override
        public String store(byte[] content, Long personId) {
            throw ute.fit.financemanagement.exception.ApiException
                    .serviceUnavailable("Chức năng lưu ảnh đại diện chưa được cấu hình");
        }

        @Override
        public String url(String key) {
            return key;
        }
    }

    static final class CloudinaryAvatarStorage implements AvatarStorage {
        private static final String PREFIX = "cloudinary:";
        private final Cloudinary cloudinary;

        CloudinaryAvatarStorage(Cloudinary cloudinary) {
            this.cloudinary = cloudinary;
        }

        @Override
        @SuppressWarnings("unchecked")
        public String store(byte[] content, Long personId) {
            try {
                String publicId = "finnest/avatars/user-" + personId;
                Map<String, Object> result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                        "resource_type", "image",
                        "type", "authenticated",
                        "public_id", publicId,
                        "overwrite", true,
                        "invalidate", true));
                return PREFIX + result.get("public_id") + ":" + result.get("version") + ":" + result.get("format");
            } catch (Exception exception) {
                throw new IllegalStateException("Không thể lưu ảnh đại diện lên Cloudinary", exception);
            }
        }

        @Override
        public String url(String key) {
            if (key == null || key.isBlank() || key.startsWith("http://") || key.startsWith("https://")
                    || !key.startsWith(PREFIX)) {
                return key;
            }
            String[] parts = key.substring(PREFIX.length()).split(":", 3);
            if (parts.length != 3) return key;
            return cloudinary.url().resourceType("image").type("authenticated")
                    .version(parts[1]).format(parts[2]).signed(true).secure(true).generate(parts[0]);
        }
    }
}
