## 1、主要工作

本周没有必做作业，所以先花大部分精力基于第9周实现的NETTY HTTP客户端完成了第三周的NETTY网关，性能还可以。在预热后，性能还可以超过HTTPCLIENT4，但这个压测结果太过于简单了。在高并发下（>40个并发，每个并发1500个请求，性能会下降，链接池出现阻塞）

网关使用连接池管理链接。搭建了一个简单的连接池。经过简单的压测，在其他条件相同的情况下，性能强于同样采用异步方式的HTTPCLIENT4。

- 对外接口 NettyHttpClient

  系统整体上通过NettyHttpClient对外提供服务，这个服务是基于底层连接池暴露的HTTP接口。遵守HTTP1.1的队头堵塞：即在一个底层的TCP连接上HTTP的请求应答的之间的生命周期不能重合。NettyHttpClient不需要处理连接，这一点和httpclient4的用法很像。

  - 根据请求的HTTP报文信息，决定目标服务器，即实现了简单的路由

    ```java
    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx)  {

        String uri = router.route(router.getEndpoints(fullRequest.uri()));

        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        //此处fullRequest经过两个流水线，会被释放两次，所以需要增加一次计数，以供客户端进行释放
        //这样可以减少不必要的内存拷贝
        ReferenceCountUtil.retain(fullRequest);

        fullRequest.setUri(url.getPath());
        fullRequest.headers()
                .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                .set(HttpHeaderNames.HOST, url.getHost() + ":" + url.getPort());

        if (url.getHost() == null) {
            System.out.println("null host!");
        }

        doHandle(fullRequest, ctx, url.getHost(), url.getPort());

    }
    ```
  - doHandle负责实际的处理

    委托给NettyConnection进行实际的通讯，通讯后，将NETTY异步接收到的结果同步应答给前端。发送完成后，将连接返回给连接池。
    ```java
    promise = connection.sendMessageByHttp(fullRequest,host, port);
    //异步转同步
    promise.get(10, TimeUnit.SECONDS);
    //System.out.println("发送响应...");
    Channel targetChannel = promise.channel();
    ClientHandler handler = (ClientHandler)targetChannel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);

    if (fullRequest != null) {
        if (!HttpUtil.isKeepAlive(fullRequest)) {
            ctx.write(handler.getResponse()).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.write(handler.getResponse());
        }
    }
    ctx.flush();
    connection.makeUsable(targetChannel);

    //测试直接归还连接
    //System.out.println("归还连接...");
    //不再连接池中的时候，才归还到连接池中
    if (!targetChannel.attr(NettyConnectionPool.IN_THE_POOL).get())
        NettyConnectionPool.getInstance().releaseChannel(targetChannel);
    ```

- NettyConnection 封装底层NETTY的Channel及其状态

  ```java
  private String host;
  private int port;
  private Channel channel;
  ```

  底层的连接由连接池进行管理

  ```java
  final NettyConnectionPool connectionPool = NettyConnectionPool.getInstance();
  ```

  - sendMessageByHttp对外暴露HTTP服务

    设置连接必要的属性，并向连接池申请连接。申请前，优先复用连接，无法复用的时候（比如当前连接正在处理一个HTTP请求），向连接池申请连接。特别的是，NettyConnection只持有一个连接，当申请连接前，需要将当前连接标记为不被自己持有
    ```java
    ChannelPromise promise = null;
    //如果连接在使用中，为了实现队头堵塞，需要获取新的连接

    if (channel == null || channel.attr(NettyConnectionPool.IN_USE_ATTR_KEY).get() == true) {
        if (channel != null) {
            //将当前连接的属主置空，这样就可以被回收到连接池中
            //System.out.println("将连接拥有者置空...");
            channel.attr(NettyConnectionPool.OWNER_KEY).set(null);
        }
        channel = connectionPool.getOneChannel(host, port, this);
    }
    ```

    然后获取Channel通道上的CLIENT_HANDLER处理器，进行发送，返回一个promise
    ```java
    ClientHandler handler = null;
    try {
        handler = (ClientHandler) channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
    }catch (Exception e) {
        System.out.println(e.getMessage());
    }

    if (handler == null) {
        System.out.println("null handler detected!");
    }

    promise = handler.sendMessage(request);
    return promise;
    ```

- ClientHandler向底层的NETTY channel上发送数据，并返回一个promise

  ```java
  /**
    * @param request 请求
    * @return 一个ChannelPromise，用于获取结果
    * @throws InterruptedException
    */
    public ChannelPromise sendMessage(FullHttpRequest request) throws InterruptedException {
        //设置对应的channel为使用状态
        ctx.channel().attr(NettyConnectionPool.IN_USE_ATTR_KEY).set(true);
        promise = ctx.newPromise();
        ctx.writeAndFlush(request);
        return promise;
    }
  ```

- NettyConnectionPool 管理TCP连接

  对不同的目的（HOTS + PORT），分开进行管理，即都有一个自己的子连接池。子连接池用ArrayBlockingQueue进行管理。每个子连接池的数量由MAX_CONNECTIONS_PER_URL进行限定

  ```java
  //存储建立的连接，键的形式为host:port，到特定的host:port可以建立多个连接
  //使用ArrayBlockingQueue而不是使用Set来存储连接，是为了实现HTTP1.1中的队头堵塞
  private final Map<String, ArrayBlockingQueue<Channel>> pool = new ConcurrentHashMap<>();
  //某个HOST + PORT的现有连接数量
  private final Map<String, AtomicInteger> numOfConnections = new ConcurrentHashMap<>();
  //管理子连接池的锁
  private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
  ```

  每个连接有若干属性
  ```java
  //当前连接是否在使用中
    public static final AttributeKey<Boolean> IN_USE_ATTR_KEY = AttributeKey.newInstance("inUse");
    //当前连接的拥有人
    public static final AttributeKey<Object> OWNER_KEY = AttributeKey.newInstance("owner");
    //当前连接是否在连接池中，用于归还连接时的去重
    public static final AttributeKey<Boolean> IN_THE_POOL = AttributeKey.newInstance("inThePool");
  ```

  使用

  - getOneChannel 上层模块可以向它申请一个连接，getOneChannel处理连接请求时，优先复用当前连接

    初始化时，要做特殊处理，以分配必要的资源
    ```java
    ArrayBlockingQueue<Channel> channelArrayBlockingQueue = pool.get(key);
    AtomicInteger currConnections = numOfConnections.get(key);
    ReentrantLock rtl = lockMap.get(key);


    if (channelArrayBlockingQueue == null) {
        synchronized (pool){

            //再次检查状态，“三位一体”
            channelArrayBlockingQueue = pool.get(key);
            currConnections = numOfConnections.get(key);
            rtl = lockMap.get(key);
            if (channelArrayBlockingQueue == null) {
                channelArrayBlockingQueue = new ArrayBlockingQueue<>(MAX_CONNECTIONS_PER_URL);
                pool.putIfAbsent(key, channelArrayBlockingQueue);

                currConnections = new AtomicInteger(0);
                numOfConnections.putIfAbsent(key, currConnections);

                rtl = new ReentrantLock();
                lockMap.putIfAbsent(key, rtl);

                //System.out.println("=======为连接 " + key + "创建锁====" + rtl.hashCode());
            }

        }
    }
    ```


    优先复用现有连接，无法复用且当前连接没有超过上限时，新建连接
    ```java
    //尝试获取连接进行复用
    Channel channel = channelArrayBlockingQueue.poll(0, TimeUnit.SECONDS);
    //如果可用连接为空，且没有达到连接数上限，创建连接
    if (channel == null) {
        System.out.println("没有获取到连接，准备新建...");
        rtl.lock();
        try{
            //再次检查连接数量
            if (currConnections.get() < MAX_CONNECTIONS_PER_URL)
                channel = createConnection(host, port, channelArrayBlockingQueue, currConnections);
        } finally {
            rtl.unlock();
        }
    }

    if (channel != null){
        try {
            //创建的连接可以直接使用，因为上面还没有尚未完成的HTTP请求
            channel.attr(OWNER_KEY).set(owner);
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
    } else {
        //连接数已经达到上限，只好从队列中获取
        //获取一个连接，当连接被使用时，阻塞调用线程，实现了所谓“队头阻塞”
        channel = channelArrayBlockingQueue.poll(10, TimeUnit.SECONDS);
        if (channel == null) {
            throw new TimeoutException("获取连接超时！");
        }else {
            System.out.println("从连接池中取到连接");
        }
    }
    ```
    并设置连接已经被取出连接池的标志
    ```java
    channel.attr(IN_THE_POOL).set(false);
    ```

  - createConnection 负责新建连接

    ```java
    /**
     * 创建连接并放入集合
     * @param host IP地址
     * @param port 端口
     * @param channelQueue 存放连接的集合
     * @param currConnections 当前连接（使用IP + PORT区分）的数量
     * @return
     */
    private Channel createConnection(String host, int port, Queue<Channel> channelQueue, AtomicInteger currConnections) {
        ChannelFuture channelFuture = null;
        Channel result = null;

        //System.out.println("线程 [" + Thread.currentThread().getId() + "]进入createConnection");

        try {
            channelFuture = bootstrap.connect(host, port).sync();
            //logger.info("Connect to {} : {} succeed: [{}]", host, port);
            //System.out.println("Connect to [" + host + ":" + port + "]succeed!");
            //System.out.println("创建了第" + currConnections.get() + "个连接");
            //增加连接计数
            currConnections.getAndIncrement();
        } catch (InterruptedException e) {
            //logger.info("Connect to {} : {} failed: [{}]", host, port, e.getLocalizedMessage());
            System.out.println("Connect to [" + host + ":" + port + "]failed! [" + e.getLocalizedMessage() + "]");
            result = null;
        }

        if (channelFuture != null) {
            channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    //关闭时移除这个连接，只有当这个连接在队列中时才移除它
                    if (channelQueue.contains(future.channel()))
                        channelQueue.remove(future.channel());
                    currConnections.decrementAndGet();
                    //logger.info("连接{}被关闭", future.channel().remoteAddress());
                    System.out.println("连接被关闭[" + future.channel().remoteAddress() +"]");
                }
            });
            result = channelFuture.channel();

            //设置channel为未使用状态
            result.attr(IN_USE_ATTR_KEY).set(false);
            //没有属主
            result.attr(OWNER_KEY).set(null);
            //还没有放入连接池
            result.attr(IN_THE_POOL).set(false);
            //下面被注释掉，新建的连接直接使用，不影响队头堵塞；连接被使用完后自动回收
            //channelQueue.add(result);
            System.out.println(host + port +"的总连接数为: " + currConnections.get());
            System.out.println("目前连接池中的连接数为[应该为0]： " + channelQueue.size());
            System.out.println("channel with id [" + result.id() + "] is created!");
        }
        System.out.println("线程 [" + Thread.currentThread().getId() + "]离开createConnection");
        return result;
    }

    ```

  - releaseChannel 负责归还连接，归还是要设置channel的状态
    ```java
    InetSocketAddress address = (InetSocketAddress)channel.remoteAddress();
    String host = address.getHostString();
    int port = address.getPort();

    String key = host + ":" + port;
    ArrayBlockingQueue<Channel> channelArrayBlockingQueue = pool.get(key);
    //此时不改变目前已经有的连接数，只是把它还回来。只有连接关闭的时候，才会改变目前的连接数
    if (channel == null) {
        System.out.println("检测到空channel!");
        return;
    }
    try {
        //设置为未使用状态
        channel.attr(IN_USE_ATTR_KEY).set(false);
        //设置属主为空
        channel.attr(OWNER_KEY).set(null);
        //设置已放入连接池标志
        channel.attr(IN_THE_POOL).set(true);
        //放入连接池
        channelArrayBlockingQueue.add(channel);
    }catch (Exception e) {
        System.out.println("发生错误，队列长度超长!");
        //e.printStackTrace();
        System.out.println("现有连接数： " + numOfConnections.get(key).get());
        System.out.println("队列长度：" + channelArrayBlockingQueue.size());
        for (Channel chl:channelArrayBlockingQueue) {
            System.out.println(chl.id());
        }
        System.exit(-1);
    }
    ```

## 2、补充第10周本周作业。20201226

- 为上周的Rpcfx增加分组和版本

  - 在上周的基础上，给注解RpcfxService增加了若干属性

  ```java
  public @interface RpcfxService {
    String url();
    String group() default "";
    String version() default "";
    String registry() default "";
    }
  ```

  然后在RpcfxInvocationHandler.RpcfxInvocationHandler#invoke中获取相关属性，并得到最终URL
  ```java
    RpcfxService rpcfxService = method.getAnnotation(RpcfxService.class);
    String group = rpcfxService.group();
    if (!group.isEmpty()) {
        String registry = rpcfxService.registry();
        String version = rpcfxService.version();
        RpcfxRegistryCenter rpcfxRegistryCenter =
                applicationContext.getBean("registry", RpcfxRegistryCenter.class);

        url = rpcfxRegistryCenter.getUrl(group, version);

    }
  ```

  RpcfxInvocationHandler.RpcfxInvocationHandler实现了ApplicationContextAware。

  - 在客户端增加类LocalRegistryConfiguration，并默认概统了一个构造函数，简化这个DEMO的处理，没有使用从配置文件注入配置值。

    ```java
    public class LocalRegistryConfiguration {
    private Map<String, List<String>> localRegistry = new ConcurrentHashMap<>();

    /**
     * 默认构造函数，这里作为DEMO，写死一个"注册表"
     */
    public LocalRegistryConfiguration() {
        List<String> redGroup = new CopyOnWriteArrayList<>();
        redGroup.add("http://localhost:8081/red/");
        redGroup.add("http://localhost:8082/red/");
        localRegistry.putIfAbsent("red", redGroup);

        List<String> blueGroup = new CopyOnWriteArrayList<>();
        blueGroup.add("http://localhost:8083/blue/");
        blueGroup.add("http://localhost:8084/blue/");
        localRegistry.putIfAbsent("blue", blueGroup);
    }

    public List<String> getGroup(String groupName) {
        return localRegistry.get(groupName);
    }

    public Set<String> getGroupName() {
        return localRegistry.keySet();
    }
    }
    ```

    - 在客户端增加RpcfxRegistryCenter的实现类LocalRegistry，从本地读取配置，并实现了Round-Robin负载算法

      ```java
        public class LocalRegistry implements RpcfxRegistryCenter {
        @Autowired
        private LocalRegistryConfiguration registryConfiguration;

        private Map<String, Integer> currentIndex = new ConcurrentHashMap<>();
        private Map<String, Integer> groupSize = new ConcurrentHashMap<>();

        @PostConstruct
        public void initIndexArray(){
            Set<String> groupNames = registryConfiguration.getGroupName();
            for (String groupName : groupNames) {
                currentIndex.putIfAbsent(groupName, 0);
                groupSize.putIfAbsent(groupName, registryConfiguration.getGroup(groupName).size());
            }
        }

        @Override
        public String getUrl(String group, String version) {
            String dstUrl = null;
            List<String> groupUrl = registryConfiguration.getGroup(group);
            if (groupUrl != null) {
                dstUrl = groupUrl.get(ThreadLocalRandom.current().nextInt() % groupSize.get(group));
            }
            return dstUrl + version + "/";
        }
    }
      ```
