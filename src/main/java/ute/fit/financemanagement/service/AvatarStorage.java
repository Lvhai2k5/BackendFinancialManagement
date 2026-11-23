package ute.fit.financemanagement.service;

public interface AvatarStorage {
    String store(byte[] content, Long personId);

    String url(String key);
}
