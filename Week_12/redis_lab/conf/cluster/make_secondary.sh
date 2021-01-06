MASTER0_ID=`redis-cli -p 7002 cluster nodes | grep 7000 | awk '{print $0}'`
MASTER1_ID=`redis-cli -p 7002 cluster nodes | grep 7001 | awk '{print $0}'`
MASTER2_ID=`redis-cli -p 7002 cluster nodes | grep 7002 | awk '{print $0}'`

redis-cli -p 8000 cluster replicate $MASTER0_ID
redis-cli -p 8001 cluster replicate $MASTER1_ID
redis-cli -p 8002 cluster replicate $MASTER2_ID

