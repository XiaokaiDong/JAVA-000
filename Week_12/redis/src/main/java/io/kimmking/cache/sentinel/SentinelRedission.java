package io.kimmking.cache.sentinel;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class SentinelRedission {

    /**
     * 创建连接池
     */
    public static RedissonClient getRedissonClient() {

        String masterName = "mymaster";

        Config redissionConfig = new Config();
        redissionConfig.useSentinelServers()
                .setMasterName(masterName)
                .addSentinelAddress("redis://127.0.0.1:26379",
                        "redis://127.0.0.1:26380");

        RedissonClient client = Redisson.create(redissionConfig);
        return client;
    }
}
