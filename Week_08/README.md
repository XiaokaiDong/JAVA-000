# 作业说明

- Week_08周四(12/03)
  - 作业 2（必做）：拆分2个库，每个库16张表。
  - 作业位置：h-sharding-jdbc
  - 说明：目前每个水平分片的数据源只支持一张表，多个表需要多个数据源，可以考虑这些表是否可以共用一个连接池
  - 代码片段

    配置数据源：包括所有库的链接、用户名、密码（多份）以及分区键的名字、表的名字以及库、表的数量
    ```
    #水平分库配置
    h_sharding.datasource.urls[0]=jdbc:mysql://127.0.01:3316/db?serverTimezone=UTC
    h_sharding.datasource.urls[1]=jdbc:mysql://127.0.01:3326/db?serverTimezone=UTC
    h_sharding.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
    h_sharding.datasource.username[0]=root
    h_sharding.datasource.password[0]=
    h_sharding.datasource.username[1]=root
    h_sharding.datasource.password[1]=
    h_sharding.datasource.numOfDatabases=2
    h_sharding.datasource.numOfTablesPerDatabase=16
    h_sharding.datasource.shardingAlgorithm=modOnKey
    h_sharding.datasource.shardingKey=order_id   //分区键名
    h_sharding.datasource.tableName=order   //表名
    h_sharding.datasource.hikari.maximumPoolSize=200
    h_sharding.datasource.hikari.minimumIdle=5
    h_sharding.datasource.hikari.idleTimeout=600000
    h_sharding.datasource.hikari.connectionTimeout=30000
    h_sharding.datasource.hikari.maxLifetime=1800000
    ```

    将上述配置通过如下方式映射到数据源属性BEAN上
    ```java
    @Bean
    @ConfigurationProperties("h_sharding.datasource")
    HShardingDataSourceProperties shardingSourceProperties() {
        return new HShardingDataSourceProperties();
    }
    ```

    数据源属性中的多个库的链接情况会被保存到DataSourceProperties列表中
    ```java
    for (int i = 0; i < urls.size(); i++) {
        DataSourceProperties dataSourceProperties = (DataSourceProperties)this.clone();
        dataSourceProperties.setUrl(urls.get(i));
        dataSourceProperties.setUsername(userNames.get(i));
        dataSourceProperties.setPassword(passwords.get(i));
        dataSourcePropertiesList.add(dataSourceProperties);
    }
    ```

    然后使用多个属性建造多个HShardingDataSource
    ```java
    public void init(HShardingDataSourceProperties shardingDataSourceProperties) {
        for (DataSourceProperties dataSourceProperties : shardingDataSourceProperties.getDataSourcePropertiesList() ) {
            DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
            dataSourceList.add(dataSource);
        }
    }
    ```

    HShardingDataSource继承自AbstractDataSource
    ```java
    public void init(HShardingDataSourceProperties shardingDataSourceProperties) {
        for (DataSourceProperties dataSourceProperties : shardingDataSourceProperties.getDataSourcePropertiesList() ) {
            DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
            dataSourceList.add(dataSource);
        }
    }
    ```

    在重载的getConnection中实现分库的路由
    ```java
    @Override
    public Connection getConnection() throws SQLException {
        return dataSourceList.get(DATASOURCE_IND.get()).getConnection();
    }
    ```

    上面的DATASOURCE_IND是一个局部变量，会在执行SQL时，根据解析出来的分区键，设置相应的数据源，下面的shardingKey是分区键
    ```java
    private HShardingJdbcTemplate withShardingKey(Object shardingKey) {
        HShardingDataSource shardingDataSource = (HShardingDataSource)jdbcTemplate.getDataSource();
        if (shardingKey != null) {
            shardingKeyHolder.set(shardingKey);
            HShardingDataSource.setDatasourceInd(modHShardingAlgorithm.getDatabaseIndex(shardingKey));
        } else {
            shardingKeyHolder.set(null);
        }
        return this;
    }
    ```

    分区键由传递的SQL解析出来，这里没有使用druid，而是手写的简易解析
    ```java
    public class SimpleSqlProcessor implements SqlProcessor {
    @Override
    public Object getShardingKey(String sql, String shardingKey, Object[] args) {
        int firstIndex = 0;
        int lastIndex = 0;
        int shardingKeyIndex = 0;

        String subString = null;

        Object shardingKeyObj = null;

        sql.toUpperCase();

        //1、先处理INSERT语句，因为INSERT语句可能不出现shardingKey所在的列。这里做简化，只处理包含列名的语句
        if (sql.indexOf("INSERT") != -1) {
            firstIndex = sql.indexOf("(");
            lastIndex = sql.indexOf(")");
            subString = sql.substring(firstIndex + 1, lastIndex).replaceAll("\\s*", "");
            String[] columns = subString.split(",");
            for (shardingKeyIndex = 0; shardingKeyIndex < columns.length; shardingKeyIndex ++) {
                if (columns[shardingKeyIndex].equals(shardingKey)) {
                    break;
                }
            }
            if (shardingKeyIndex == columns.length) {
                return null;
            } else {
                return args[shardingKeyIndex];
            }
        }

        //2、处理UPDATE、DELETE、SELECT，在WHERE后面寻找
        if (sql.indexOf("SELECT") != -1 || sql.indexOf("DELETE") != -1 || sql.indexOf("UPDATE") != -1) {
            //寻找WHERE
            firstIndex = sql.indexOf("WHERE");
            if(firstIndex != -1) {
                //在WHERE后寻找分区键
                lastIndex = sql.indexOf(shardingKey, firstIndex);
                //把分区键后的内容按照等号分割, 结果数组中第2个元素就包含了分区键的值
                String[] conditions = sql.substring(lastIndex).split("=");

                //记住分区键后的第一个"？"的位置，后面使用
                int indexTmp = sql.indexOf('?', lastIndex);

                //分割出分区键,按空格分割，第一个元素就是分区键
                String shardingKeyName = conditions[1].trim().split("\\s")[0];
                if (!shardingKeyName.equals("?")) {
                    //如果条件是常量
                    return shardingKeyName;
                } else {
                    //得到分区键值的序号
                    if (indexTmp == -1){
                        //SQL语句中不包含?，语句有问题
                        return null;
                    } else {
                        //找到SQL字符串中第一个"?"
                        firstIndex = sql.indexOf('?');
                        subString = sql.substring(firstIndex, indexTmp + 1);
                        shardingKeyIndex = 0;
                        firstIndex = 0;
                        while((firstIndex = subString.indexOf('?', firstIndex)) != -1) {
                            shardingKeyIndex ++;
                        }
                        return args[shardingKeyIndex];
                    }
                }
            } else {
                return null;
            }
        }

        return null;
    }

    }
    ```

    再加上动态重写表名，来实现分库分表
    ```java
    //定位某一张表，不使用SQL解析的简化替代方案
    private HShardingJdbcTemplate withTableName(String tableName) {
        int tableIndex = 0;
        if (shardingKeyHolder.get() != null) {
            tableIndex = modHShardingAlgorithm.getTableIndex(shardingKeyHolder.get());
            String targetTableName = String.format("_%02d", tableIndex);
            sql.replace(tableName, targetTableName);
        }
        return this;
    }

    private void preProcess(Object... args){
        String shardingKey = ((HShardingDataSource)jdbcTemplate.getDataSource()).getShardingKey();
        String tableName = ((HShardingDataSource)jdbcTemplate.getDataSource()).getTableName();
        withShardingKey(sqlProcessor.getShardingKey(sql,shardingKey,args))
                .withTableName(tableName);
    }

    public int update(@Nullable Object... args) {

        preProcess(args);

        if (shardingKeyHolder.get() == null){
            //广播SQL
            return 0;
        }else {
            log.info("the actual sql is " + sql );
            return jdbcTemplate.update(sql, args);
        }
    }
    ```

- Week_08周六(12/05)
  - 作业 2（选做6）：TCC事务框架。
  - 提交截止点没有做完，后续完善（20201209）
  - 20201211进行了初步完善

    主要使用了JAVA的CompletableFuture进行开发，目前还只是一个原型，但是基本功能点已经完备了。大体想法如下：

    根据请求生成TransactionProperties
    ```java
    public interface Request {
    TransactionProperties getTransactionProperties();
    }
    ```

    然后根据TransactionProperties得到TransactionBuilder
    ```java
    TransactionBuilder<?> initializeTransactionBuilder();
    ```

    然后就可以构建事务了
    ```java
    public interface TransactionBuilder<T extends Transaction> {
    T build();
    }
    ```

    事务管理器作为系统的主控将事务提交个线程池执行
    ```java
    public class AbstractTransactionManager implements TransactionManager {

    //执行任务的线程池
    private ExecutorService executorService;

    //接收请求的单端阻塞队列
    private Queue<Request> requests = new ArrayBlockingQueue<>(100);

    public AbstractTransactionManager(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public Transaction buildTransactionFrom(Supplier<TransactionProperties> transactionPropertiesFactory) {
        TransactionProperties transactionProperties = transactionPropertiesFactory.get();
        return transactionProperties.initializeTransactionBuilder().build();
    }

    @Override
    public void startTccTx(Transaction transaction) {
        executorService.submit(transaction);
    }

    @Override
    public boolean take(Supplier<Request> requestFactory) {
        return requests.add(requestFactory.get());
    }

    @Override
    public void serve() {
        Request request = requests.poll();
        Transaction transaction = buildTransactionFrom(() -> request.getTransactionProperties());
        executorService.submit(transaction);
    }


    }
    ```

    每个事务定义如下
    ```java
    public interface Transaction extends Runnable{
    /**
     * 返回事务ID
     * @return 事务ID
     */
    String getTxID();

    /**
     * 设置线程池，实现事物之间的隔离
     * @param executor 线程池
     */
    void setExecutor(Executor executor);

    /**
     * 设置事务的上下文，避免在TCC服务间频繁传递参数
     * @param request
     */
    void setContext(Request request);

    }
    ```

    实现了一个只能串行执行的事务（比如支付系统）
    ```java
    public class BasicSerialTransaction implements Transaction {
    private String txID;

    private Request request;

    //需要cancel的任务数量
    private AtomicInteger cancelIndex = new AtomicInteger(0);

    //重试次数
    private AtomicInteger retryTime = new AtomicInteger(0);

    //最大重试次数
    private final static int maxRetryTime = 3;

    //是否允许各个参与的服务并发执行
    private boolean paralleled = false;

    //这里TCC事务涉及到的各个参与方是有顺序的，在builder中应该按照Transaction的定义按序添加TccService
    private List<TccService> participator;

    //执行TCC服务的栈
    private Deque<CompletableFuture<Void>> tasksStack = new ConcurrentLinkedDeque<>();

    //当前执行的TCC服务
    private AtomicInteger currTccService = new AtomicInteger(0);

    //执行任务的线程池
    private Executor executor;

    //需要在confirm阶段重试的参与方
    private Set<TccService> needReconfirmServices = new ConcurrentSkipListSet<>();

    //只允许用相应的Builder进行建造
    private BasicSerialTransaction() {}

    @Override
    public String getTxID() {
        return txID;
    }

    @Override
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void setContext(Request request) {
        this.request = request;
    }

    private void init() {
        for (TccService tccService: participator ) {
            tccService.tccStart(txID);
        }

    }

    /**
     * 执行这个事物
     */
    @Override
    public void run() {
        init();

        if (participator.size() == 0)
            return;

        tryIt();

        if (cancelIndex.get() > 0) {
            cancel();
        } else {
            while (maxRetryTime > 0 && !needReconfirmServices.isEmpty()) {
                confirm();
            }
        }

    }

    /**
     * try阶段
     */
    private void tryIt() {
        //使用栈进行
        CompletableFuture<Void> head = CompletableFuture
                .runAsync(() -> {
                    participator.get(currTccService.get()).tccTry(request);
                }, executor)
                .thenRunAsync(() -> {
                    this.needReconfirmServices.add(participator.get(currTccService.get()));
                }, executor)
                .exceptionally((e) -> {      //如果发生异常，则1、结束整个流程；2、cancel自己
                    log.info("The service [{}] failed because: {}",
                            participator.get(currTccService.get()).getName(),
                            e.getMessage());

                    //try发生异常，直接结束处理
                    currTccService.set(participator.size());
                    //cancel，记录需要cancel的任务数量，由取消线程统一处理
                    cancelIndex.set(currTccService.get());

                    return null;
                });

        //放入堆栈，开启流程
        tasksStack.push(head);

        CompletableFuture<Void> task = null;
        do {
            task = tasksStack.pop();
            if(currTccService.get() < participator.size()) {
                //取下一个分布式事务的参与方
                task.thenRunAsync(() -> {
                    participator.get(currTccService.incrementAndGet()).tccTry(request);
                }, executor)
                    //如果成功,则放入confirm集合
                    .thenRunAsync(() -> {
                        needReconfirmServices.add(participator.get(currTccService.get()));
                    }, executor)
                    .exceptionally((e) -> {
                        log.info("Try service [{}] failed because: {}",
                                participator.get(currTccService.get()).getName(),
                                e.getMessage());

                        //try发生异常，直接结束处理
                        currTccService.set(participator.size());
                        //cancel，记录需要cancel的任务数量，由取消线程统一处理
                        cancelIndex.set(currTccService.get());

                        return null;
                    });
                tasksStack.push(task);
            }

        } while(task != null);
    }

    private void cancel() {
        //进行cancel处理
        while (cancelIndex.get() > 0) {
            participator.get(cancelIndex.decrementAndGet()).tccCancel(request);
        }
    }

    private void confirm() {
        Iterator<TccService> it = needReconfirmServices.iterator();
        while(it.hasNext()) {
            TccService service = it.next();
            CompletableFuture
                    .runAsync(() -> {service.tccConfirm(request);})
                    .thenRunAsync(() -> needReconfirmServices.remove(service))
                    .exceptionally((e) -> {
                        log.info("the service confirm failed because {}, will retry for [{}] round",
                                e.getCause(), retryTime.get());
                        return null;
                    });
        }
        retryTime.incrementAndGet();
    }
    }
    ```

    由事务驱动TccService进行交易
    ```java
    public interface TccService {
    <T> void tccTry(T tryContext);
    <T> void tccConfirm(T confirmContext);
    <T> void tccCancel(T cancelContext);

    /**
     * 初始化一个服务，初始化需要将这个服务的状态等相关信息序列化，比如序列化到数据库中或者消息队列中
     * @param txId，所属交易的ID
     */
    void tccStart(String txId);
    String getName();
    }
    ```