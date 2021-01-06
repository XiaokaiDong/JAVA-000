package io.kimmking.cache.lock.redis;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LockByRedis {
    private String lock_key = "redis_lock"; //锁键

    protected long lockDuration = 10;//锁过期时间

    private long tryLockTimeout = 3; //获取锁的超时时间

    //依赖于spring boot自动配置的JedisConnectionFactory
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    /**
     *
     * @param id 锁的标识，防止锁被错误地释放
     * @return 获取成功则返回true, 否则返回false
     */
    public boolean lock(String id) {
        RedisConnection redisConnection = redisConnectionFactory.getConnection();
        Long start =System.currentTimeMillis();
        try {
            while (true) {
                Boolean locked = redisConnection
                        .stringCommands()
                        .set(lock_key.getBytes(), id.getBytes(),
                                Expiration.seconds(lockDuration),
                                RedisStringCommands.SetOption.SET_IF_ABSENT);
                if (locked) {
                    return true;
                }
                //否则循环等待，在timeout时间内仍未获取到锁，则获取失败
                long l = System.currentTimeMillis() - start;
                if (l >= tryLockTimeout) {
                    return false;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error("分布式锁异常", e);
                }
            }
        } finally {
            redisConnection.close();
        }
    }

    public boolean unlock(String id) {
        RedisConnection redisConnection = redisConnectionFactory.getConnection();
        String script =
                "if redis.call('get',KEYS[1]) == ARGV[1] then" +
                        "   return redis.call('del',KEYS[1]) " +
                        "else" +
                        "   return 0 " +
                        "end";

        try {
            int result = redisConnection
                    .scriptingCommands()
                    .eval(script.getBytes(), ReturnType.INTEGER, 1, lock_key.getBytes(), id.getBytes());


            if (result == 1) {
                return true;
            }
            return false;
        } finally {
            redisConnection.close();
        }
    }
}
