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