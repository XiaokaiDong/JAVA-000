week 15作业 - 一个秒杀系统

参照架构图”秒杀“。

秒杀开始前，将库存均匀的导入REDIS集群，平均分配库存。

0、用户访问部署在CDN上的静态化的秒杀页面。

1、秒杀开始后，动态请求到达负载均衡处。

2、请求经过负载，到达API网关，完成认证、鉴权、限流、熔断等功能。对所有请求，赋予一个唯一的编号。由于网关上的连接非常多，采用异步处理。

3、对于正常的请求，将其丢给MQ，进行削峰和排序。线程返回，处理下一个连接上的请求。

4、秒杀系统集群从MQ中取出请求。

5、到REDIS中查询库存，利用REDIS的单线程特性对库存进行原子性的扣减。

6、扣减成功后，将请求放入与订单相关的MQ中。

7、订单系统从MQ中取出消息，生成订单。

8.1、订单系统将购买成功的消息放入MQ，以通知网关。
8.2、订单系统将订单放入MQ，供支付系统拉取，进行支付。

9、网关上的线程异步的从MQ中取出秒杀结果，根据消息的ID，应答前端。对于超时且没有秒杀成功的请求，也应答前端秒杀失败。

10、用户根据秒杀结果，进入支付系统，拉取、展示订单，发起支付。

11、支付系统完成支付。购买结束。