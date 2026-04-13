package com.erandevu.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisContainerProvider {
    private static GenericContainer<?> redisContainer;

    static {
        // 1. Redis TestContainer singleton
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:6.2.6"))
                .withExposedPorts(6379)
                .withReuse(true); // Use the same container across tests if possible
    }

    public static void startContainer() {
        // 2. Container initialization for cache tests
        if (!redisContainer.isRunning()) {
            redisContainer.start();
        }
    }

    public static void stopContainer() {
        // Clean up the container after tests
        if (redisContainer.isRunning()) {
            redisContainer.stop();
        }
    }

    // 3. Connection configuration
    public static String getConnectionUrl() {
        return "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379);
    }

    // 4. Cache cleanup utilities
    public static void clearCache() {
        // Implement Redis cache cleanup logic here
        // Example: RedisCommands.sync().flushdb();
    }
}