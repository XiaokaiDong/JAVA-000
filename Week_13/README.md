## 13周作业

### 周四作业第1题、第2题

- 综合考虑这两道题，它们对外都表现为一个消息队列，所以先写一个抽象的公共接口，位于template下

  - 定义抽象接口MessagingTransporter

    ```java
    /**
     * 消息传递的泛型接口
    * @param <SESSION>  消息传递的上下文，一般位于底层的连接之上
    * @param <DESTINATION>  消息容器的名字
    */
    public interface MessagingTransporter<SESSION, DESTINATION> {

        void close();
        void initConnectionFactory();

        SESSION getSession();

        /**
        * 处理消息，使用了回调模式
        * @param processor  处理器
        * @param session    会话
        * @param destination 消息的标的
        */
        default void processMessage(BiConsumer<SESSION, DESTINATION> processor,
                                    SESSION session, DESTINATION destination){
            processor.accept(session, destination);
        }

        /**
        * 处理消息，使用了回调模式
        * @param producingProcessor  处理器
        * @param session    会话
        * @param destination 消息的标的
        */
        default void produceMessage(ProducingProcessor<SESSION, DESTINATION> producingProcessor,
                                    SESSION session, DESTINATION destination) {
            processMessage(producingProcessor, session, destination);
        }

        /**
        * 处理消息，使用了回调模式
        * @param consumingProcessor  处理器
        * @param session    会话
        * @param destination 消息的标的
        */
        default void consumeMessage(ConsumingProcessor<SESSION, DESTINATION> consumingProcessor,
                                    SESSION session, DESTINATION destination) {
            processMessage(consumingProcessor, session, destination);
        }
    }
    ```

    - 其中processMessage的设计采用了回调的方式，实现者需要传递BiConsumer实例进行消息的处理

    - processMessage的“便捷方式”produceMessage、consumeMessage的使用了ProducingProcessor、ConsumingProcessor。它们其实是BiConsumer的“类型别名”

        ```java
        public interface ProducingProcessor <SESSION, DESTINATION> extends BiConsumer<SESSION, DESTINATION> {
        }

        public interface ConsumingProcessor<SESSION, DESTINATION> extends BiConsumer<SESSION, DESTINATION> {
        }
        ```

  - 基于JMS实现MessagingTransporter，位于jms包下

    - 首先定义实现了MessagingTransporter的抽象类JmsMessagingTransporter

      ```java
        /**
         * 基于JMS实现消息传送
        * 使用JMS的Session表示上下文，即JMS的会话
        * 使用JMS的Destination表示消息的容器，可以是队列\分区，也可以是主题
        */
        @Slf4j
        @Data
        abstract public class JmsMessagingTransporter implements MessagingTransporter<Session, Destination> {
            protected String uriBroker;
            protected ConnectionFactory connectionFactory;
            protected Session session;
            private Connection connection;

            protected JmsMessagingTransporter(String uriBroker) {
                this.uriBroker = uriBroker;
                init();
            }

            private void init(){
                if (this.uriBroker == null){
                    log.info("The broker has not been designated, init failed!");
                }

                try {
                    initConnectionFactory();
                    connection = connectionFactory.createConnection();
                    connection.start();
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                } catch (JMSException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void close(){

                try {
                    session.close();
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }

            }

            /**
            * 初始化连接工厂，子类实现。
            */
            @Override
            abstract public void initConnectionFactory();


        }
      ```

      JmsMessagingTransporter基于模板模式，因为它基于JMS，而JMS本身就是一个接口，子类要实现initConnectionFactory方法类似实现抽象工厂

    - 使用activemq写一个具体类ActivateMQJmsMessagingTransporter,实现initConnectionFactory
        ```java
        public class ActivateMQJmsMessagingTransporter extends JmsMessagingTransporter {
            public ActivateMQJmsMessagingTransporter(String uriBroker) {
                super(uriBroker);
            }

            @Override
            public void initConnectionFactory() {
                super.connectionFactory = new ActiveMQConnectionFactory(super.uriBroker);
            }

        }
        ```

    - 定义生产者消息处理器ActiveMQProducingProcessor
        ```java
        public class ActiveMQProducingProcessor implements ProducingProcessor<Session, Destination> {

            @Override
            public void accept(Session session, Destination destination) {
                // 生产100个消息
                try {
                    MessageProducer producer = session.createProducer(destination);
                    int index = 0;
                    while (index++ < 100) {
                        TextMessage message = session.createTextMessage(index + " message.");
                        producer.send(message);
                    }
                }catch (JMSException e) {
                    e.printStackTrace();
                }
            }

        }
        ```

    - 定义消费者消息处理器ActiveMQConsumingProcessor

        ```java
        public class ActiveMQConsumingProcessor implements ConsumingProcessor<Session, Destination> {

            @Override
            public void accept(Session session, Destination destination) {
                try {
                    // 创建消费者
                    MessageConsumer consumer = session.createConsumer( destination );
                    final AtomicInteger count = new AtomicInteger(0);
                    MessageListener listener = new MessageListener() {
                        public void onMessage(Message message) {
                            try {
                                // 打印所有的消息内容
                                // Thread.sleep();
                                System.out.println(count.incrementAndGet() + " => receive from " + destination.toString() + ": " + message);
                                // message.acknowledge(); // 前面所有未被确认的消息全部都确认。

                            } catch (Exception e) {
                                e.printStackTrace(); // 不要吞任何这里的异常，
                            }
                        }
                    };
                    // 绑定消息监听器
                    consumer.setMessageListener(listener);

                    //consumer.receive()
                }catch (JMSException e) {

                }
            }
        }
        ```

  - 基于数据库实现MessagingTransporter，位于database包下。和基于JMS的实现思路是一样的

    - 定义实现了MessagingTransporter的抽象类DatabaseMessagingTransporter，也是一个抽象类

        ```java
        /**
         * 基于数据库实现消息传送
        * 使用JDBC的的Connection表示上下文，即数据库的会话
        * 使用String表示消息的容器——数据库的消息表中消息类别字段
        */
        @Slf4j
        @Data
        abstract public class DatabaseMessagingTransporter implements MessagingTransporter<Connection, String> {
            protected String urlBroker;
            protected DataSource connectionFactory;
            private Connection session;
            //protected Statement session;


            protected DatabaseMessagingTransporter(String urlBroker) {
                this.urlBroker = urlBroker;
                init();
            }

            private void init() {
                if (this.urlBroker == null){
                    log.info("The broker has not been designated, init failed!");
                }

                initConnectionFactory();

                try {
                    session = connectionFactory.getConnection();
                    //session = connection.createStatement();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();

                }
            }

            public void close() {

                try {
                    session.close();
                    //session.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }

            /**
            * 初始化连接工厂，子类实现。
            */
            abstract public void initConnectionFactory();

        }
        ```

    - 使用MySQL写一个具体类MySqlMessagingTransporter，此时连接工厂就是DataSource，利用SPRING注入一个
    
        ```java
        public class MySqlMessagingTransporter extends DatabaseMessagingTransporter {
            public MySqlMessagingTransporter(String urlBroker) {
                super(urlBroker);
            }

            @Autowired
            DataSource dataSource;

            @Override
            public void initConnectionFactory() {
                super.connectionFactory = dataSource;
            }


        }
        ```


    - 定义生产者消息处理器MySqlProducingProcessor

        ```java
        /**
         * 基于MySql的消息生产者处理器
        * 为了实现简单，直接
        */
        public class MySqlProducingProcessor implements ProducingProcessor<Connection, String> {
            @Override
            public void accept(Connection connection, String destination) {
                String insertSql = "insert into tb_messages (destination, content, " +
                        "processed, create_time" +
                        "values (?, ?, ?, ?)";

                try {
                    PreparedStatement stmt = connection.prepareStatement(insertSql);
                    for (int i = 0; i < 100; i++) {
                        stmt.setString(1, destination);
                        stmt.setString(2,i + " message.");
                        stmt.setInt(3, 0);
                        stmt.setLong(4, System.currentTimeMillis());
                        stmt.executeUpdate();
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        ```


    - 定义消费者消息处理器MySqlConsumingProcessor

      - 在消费的时候，按批次（每批20个）取出消息后，先把这20个消息打上自己的标识，将其状态标记为处理中
      - 处理后，将所有这些消息的状态标记为处理完

        ```java
        public class MySqlConsumingProcessor implements ConsumingProcessor<Connection, String> {
            private final static String processorID = System.getenv("HOSTNAME") + "_" + Thread.currentThread().getId();

            @Override
            public void accept(Connection connection, String destination) {
                //标记数据，相当于打一个快照
                String markSql = "update tb_messages set processed = 2, processor_id = ? where destination = ?" +
                        "and processed = 0 limit 20";

                //读取打了标记的快照
                String querySql = "select content from tb_messages where destination = ?" +
                        " and processed = 2 and processor_id = ? order by create_time";

                //对处理过的数据进行打标
                String makeProcessed = "update tb_messages set processed = 1, processor_id = ? where destination = ?" +
                        "and processed = 2 limit 20";

                String message = null;
                try {
                    PreparedStatement markStmt = connection.prepareStatement(markSql);
                    markStmt.setString(1, processorID);
                    markStmt.setString(2, destination);
                    markStmt.executeUpdate();

                    PreparedStatement stmt = connection.prepareStatement(querySql);
                    stmt.setString(1, destination);
                    stmt.setString(2, processorID);
                    ResultSet rs = stmt.executeQuery();
                    while(rs.next()) {
                        message = rs.getString(1);
                        System.out.println(" => receive from " + destination + ": " + message);
                    }

                    PreparedStatement processedStmt = connection.prepareStatement(makeProcessed);
                    processedStmt.setString(1, processorID);
                    processedStmt.setString(2, destination);
                    processedStmt.executeUpdate();

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        ```










  

### 周六作业第一题必做题

- 位于kafka-cluster目录下

  基于老师给的示范代码，自己搭建KAFKA集群，配置文件分别是server9092.properties、server9093.properties、server9094.properties

  