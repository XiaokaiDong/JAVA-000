package io.kimmking.cache.sentinel;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.clients.jedis.HostAndPort;

import java.util.HashSet;
import java.util.Set;

public class SentinelLettuce {

    private static LettuceConnectionFactory POOL = createLettuceConnectionFactory();

    /**
     * 创建连接池
     */
    private static LettuceConnectionFactory createLettuceConnectionFactory() {

        String masterName = "mymaster";
        Set<String> sentinels = new HashSet<String>();
        sentinels.add(new HostAndPort("127.0.0.1",26379).toString());
        sentinels.add(new HostAndPort("127.0.0.1",26380).toString());

        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration(masterName, sentinels);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisSentinelConfiguration);
        return factory;
    }

    public static RedisConnection getRedisConnection() {
        return POOL.getConnection();
    }

    public static RedisSentinelConnection getRedisSentinelConnection() {
        return POOL.getSentinelConnection();
    }
}
