# 12周作业

- 必做作业所需的脚本在redis_lab目录下。

  - 主从复制、sentinel高可用课上老师基本完成了，这里只是修改了工作目录，就是redis_lab目录下的四个conf配置文件。

  - 集群配置参考了老师PPT给出的链接，改成了脚本

    - 集群建立命令在redis_lab/cluster目录下。

    - 使用meet命令构造集群节点（meet.sh）

    ```shell
    #为redis建立集群
    redis-cli -p 7000 cluster meet 127.0.0.1 7001
    redis-cli -p 7000 cluster meet 127.0.0.1 7002
    redis-cli -p 7000 cluster meet 127.0.0.1 8000
    redis-cli -p 7000 cluster meet 127.0.0.1 8001
    redis-cli -p 7000 cluster meet 127.0.0.1 8002

    ```

    - 使用cluster addslots命令分配哈希槽(add_slots.sh、distr_slots_to_node.sh)

    - 使用make_secondary.sh建立从节点

    - make_cluster.sh为主控脚本，调用其余脚本

  - 选做作业：练习示例代码中的作业题

    - 参考C2，实现基于Lettuce和Redission的Sentinel配置

      先是SentinelLettuce
      ```java
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
      ```

      SentinelLettuce的使用如下
      ```java
      RedisConnection redisConnection = SentinelLettuce.getRedisConnection();
      System.out.println(redisConnection.info());
      redisConnection.set("uptime".getBytes(), new Long(System.currentTimeMillis()).toString().getBytes());
      System.out.println(redisConnection.get("uptime".getBytes()));
      ```

      然后是SentinelRedission
      ```java
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
      ```

      SentinelRedission的使用如下：
      ```java
      RedissonClient redissonClient = SentinelRedission.getRedissonClient();
      RBucket<String> keyObject = redissonClient.getBucket("uptime");
      keyObject.set(new Long(System.currentTimeMillis()).toString());
      ```      

    - 实现springboot/spring data redis的sentinel配置
    ```yml
    spring:
      redis:
        sentinel:
          master: mymaster
          nodes: 127.0.0.1:26379,127.0.0.1:26380
        lettuce:
          pool:
            max-idle: 10
            max-active: 20
            min-idle: 5
            max-wait: 10000ms
    ```