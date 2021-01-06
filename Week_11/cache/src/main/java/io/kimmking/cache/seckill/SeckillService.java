package io.kimmking.cache.seckill;

import io.kimmking.cache.lock.redis.LockByRedis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 支持秒杀业务的类
 */
public class SeckillService {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 使用Lua脚本来进行秒杀，保证操作的原子性
     * @param itemName
     * @param number
     * @return
     */
    public Integer lockSomething(String itemName, int number) {
        RedisConnection redisConnection = redisConnectionFactory.getConnection();

        String script =
                "local counts = redis.call('HMGET', KEYS[1], 'total', 'ordered')" + //获取商品库存信息
                        "local total = tonumber(counts[1])" +                       //将总库存转换为数值
                        "local ordered = tonumber(counts[2])" +                     //将已被秒杀的库存转换为数值
                        "if ordered + k <= total then" +                            //如果当前请求的库存量加上已被秒杀的库存量仍然小于总库存量，就可以更新库存
                        "    redis.call('HINCRBY',KEYS[1],'ordered',k)" +           //更新已秒杀的库存量
                        "    return k;" +
                        "end" +
                        "return 0";

        try {
            Integer result = redisConnection
                    .scriptingCommands()
                    .eval(script.getBytes(), ReturnType.INTEGER, 1, itemName.getBytes(), String.valueOf(number).getBytes());

            return result;
        } finally {
            redisConnection.close();
        }
    }

    /**
     * 使用分布式锁完成抢购
     * @param locker 分布式锁
     * @param itemName 商品名称
     * @param number  抢购数量
     * @return
     */
    public Integer lockSomethingWithDisLock(LockByRedis locker, String itemName, int number) {

        try {
            boolean locked = locker.lock(InetAddress.getLocalHost().getHostName() + Thread.currentThread().getId());
            if (locked) {
                //尚未完成

                return number;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
