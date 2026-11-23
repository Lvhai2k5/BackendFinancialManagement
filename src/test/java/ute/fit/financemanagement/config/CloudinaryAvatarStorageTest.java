package ute.fit.financemanagement.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.cloudinary.Cloudinary;

class CloudinaryAvatarStorageTest {

    @Test
    void createsHttpsSignedDeliveryUrlForAuthenticatedAsset() {
        var storage = new AvatarStorageConfig.CloudinaryAvatarStorage(
                new Cloudinary("cloudinary://test-key:test-secret@demo"));

        String url = storage.url("cloudinary:finnest/avatars/user-8:1785000000:png");

        assertTrue(url.startsWith("https://res.cloudinary.com/demo/image/authenticated/s--"));
        assertTrue(url.contains("/v1785000000/finnest/avatars/user-8.png"));
        assertTrue(url.contains("/finnest/avatars/user-8.png"), url);
    }
}
