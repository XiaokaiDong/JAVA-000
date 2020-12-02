学习笔记

### 1、必做作业1在readwrite-jdbc下

#### 1.1、MultiDataSourceProperties

MultiDataSourceProperties继承自DataSourceProperties，配置语法也类似，但可以一次性的配置多个数据url，这些url保存在列表中。
但对主数据源应该只配置一个，因为还没有保证多主一致性的方法。

#### 1.2、MultiDataSources

MultiDataSources继承自AbstractDataSource，可以根据MultiDataSourceProperties进行建造，故可以保存多个实际的数据源。
实现了抽象方法getConnection，根据路由策略实现多个数据元的路由，用于多个从库的情形。

#### 1.3、DynamicDataSource

DynamicDataSource继承自AbstractRoutingDataSource，具体实现动态数据源的切换。

#### 1.4、RoutingStrategy

RoutingStrategy负责MultiDataSources内多个数据元的路由，目前有两个实现：RoundRobinRoutingStrategy和TrivialRoutingStrategy，
分别对应RoundRobin算法和使用第一个算法。

#### 1.5、 主从数据源的切换

使用注解配合AOP进行切换，比如

`@ReadOnly(readonly = true)`
`@RoutingStrategy(name = "RR")`

`public void selectSth()`

### 2、必做作业2在my-shardingsphere-jdbc下

参照网上的例子以及官方手册进行
