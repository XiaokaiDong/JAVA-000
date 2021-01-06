#为redis建立集群
redis-cli -p 7000 cluster meet 127.0.0.1 7001
redis-cli -p 7000 cluster meet 127.0.0.1 7002
redis-cli -p 7000 cluster meet 127.0.0.1 8000
redis-cli -p 7000 cluster meet 127.0.0.1 8001
redis-cli -p 7000 cluster meet 127.0.0.1 8002

