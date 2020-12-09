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
  - 提交截止点没有做完，后续完善