# 11周作业

- 基于11周课程示例代码cache完成

  分布式锁的代码位于 lock/redis/LockByRedis.java中。在指定的超时时间内获取锁。获取锁的过程就是利用了REDIS的set命令。

  使用RedisConnection操作REDIS，可以根据配置使用Jedis或者Lettuce，也可以稍加改动支持哨兵模式或者集群。

- 上锁操作

  ```java
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
  ```

- 解锁操作

  使用lua脚本保证原子性。

  ```java
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
  ```

- 秒杀场景扣减库存

  - 利用LUA脚本保证查询库存、扣减库存的原子性

    ```java
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
    ```
  - 还可以利用分布式锁保证扣减库存的原子性

  
  