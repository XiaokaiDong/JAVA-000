redis-server /opt/config/redis_7000.conf
redis-server /opt/config/redis_7001.conf
redis-server /opt/config/redis_7002.conf


redis-server /opt/config/redis_8000.conf
redis-server /opt/config/redis_8001.conf
redis-server /opt/config/redis_8002.conf

./meet.sh
./distr_slots_to_node.sh
./make_secondary.sh

