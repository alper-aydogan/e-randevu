package com.erandevu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache yönetim servisi.
 * Cache istatistikleri ve yönetimi için kullanılır.
 * Production ortamında monitoring ve troubleshooting için faydalıdır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementService {

    private final CacheManager cacheManager;

    /**
     * Tüm cache'leri temizler.
     */
    public void clearAllCaches() {
        log.info("Clearing all caches");
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", name);
            }
        });
        log.info("All caches cleared successfully");
    }

    /**
     * Belirli bir cache'i temizler.
     *
     * @param cacheName cache adı
     */
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cache cleared: {}", cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }

    /**
     * Tüm cache'lerin istatistiklerini döndürür.
     *
     * @return cache istatistikleri
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();

        stats.put("totalCaches", cacheNames.size());
        stats.put("cacheNames", cacheNames);

        Map<String, Object> cacheDetails = new HashMap<>();
        for (String name : cacheNames) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("name", name);
                cacheInfo.put("type", cache.getClass().getSimpleName());
                cacheDetails.put(name, cacheInfo);
            }
        }
        stats.put("caches", cacheDetails);

        log.debug("Cache statistics retrieved: {} caches", cacheNames.size());
        return stats;
    }

    /**
     * Belirli bir cache hakkında bilgi döndürür.
     *
     * @param cacheName cache adı
     * @return cache bilgisi
     */
    public Map<String, Object> getCacheInfo(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        Map<String, Object> info = new HashMap<>();

        if (cache == null) {
            info.put("error", "Cache not found: " + cacheName);
            return info;
        }

        info.put("name", cacheName);
        info.put("type", cache.getClass().getSimpleName());

        // Native cache erişimi (Redis, Caffeine, vs.)
        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof ConcurrentMap<?, ?> mapCache) {
            info.put("estimatedSize", mapCache.size());
        }

        log.debug("Cache info retrieved: {}", cacheName);
        return info;
    }

    /**
     * Cache sağlık kontrolü.
     *
     * @return true tüm cache'ler sağlıklı
     */
    public boolean isCacheHealthy() {
        try {
            for (String name : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(name);
                if (cache == null) {
                    log.warn("Cache not accessible: {}", name);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Cache health check failed", e);
            return false;
        }
    }
}
