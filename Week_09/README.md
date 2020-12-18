- 第一次作业
  - （必做）改造自定义RPC的程序，提交到github： 
    作业位于rpc01下，基于作业模板修改而来
    - 1）尝试将服务端写死查找接口实现类变成泛型和反射

        课上秦老师已经将这一步做得差不多了

    - 2）尝试将客户端动态代理改成AOP，添加异常处理

        客户端代理使用Bytebuddy实现

        ```java
        public class RpcfxAop {
            private static Map<String, Object> agentCache = new ConcurrentHashMap<>();

            public static <T> T create(final Class<T> serviceClass, final String url)  {

                String className = serviceClass.getName().concat("Agent");

                try {
                    return (T) new ByteBuddy()
                            .subclass(Object.class)
                            .implement(serviceClass)
                            .intercept(InvocationHandlerAdapter.of(new Rpcfx.RpcfxInvocationHandler(serviceClass, url)))
                            .name(className)
                            .make()
                            .load(RpcfxAop.class.getClassLoader())
                            .getLoaded()
                            .getDeclaredConstructor()
                            .newInstance();
                } catch (Exception e) {
                    System.out.println("Cannot create a agent of class " + serviceClass.getName());
                    return null;
                }

            }
        }

        ```

    - 3）尝试使用Netty+HTTP作为client端传输方式

        实现了一个基于NETTY的HTTP客户端：

        用链接池管理链接，实现了HTTP1.1的队头堵塞

        ```java

        //可以向每个目标URL发起的最大连接数（和CHROME类似，限制向每个网站发起的连接数为6个）
        private final static int MAX_CONNECTIONS_PER_URL = 6;

        //存储建立的连接，键的形式为host:port，到特定的host:port可以建立多个连接
        //使用ArrayBlockingQueue而不是使用Set来存储连接，是为了实现HTTP1.1中的队头堵塞
        private Map<String, ArrayBlockingQueue<Channel>> pool = new ConcurrentHashMap<>();
        ```

        需要时从池冲获取链接
        ```java
        /**
         * 从连接池中获取一个共HTTP1.1使用的连接，当这个连接不存在时，创建；当连接存在时直接使用，
        * 注意HTTP1.1存在队头阻塞的现象，一个TCP连接上，不同的HTTP请求应答声明周期不可以重合
        * @param host 主机地址
        * @param port 端口
        * @return
        */
        public Channel getOneChannel(String host, int port) {
            String key = host + ":" + port;
            ArrayBlockingQueue<Channel> channelArrayBlockingQueue = pool.get(key);

            if (channelArrayBlockingQueue == null) {
                channelArrayBlockingQueue = new ArrayBlockingQueue<>(MAX_CONNECTIONS_PER_URL);
                pool.putIfAbsent(key, channelArrayBlockingQueue);
            }

            Channel channel = null;

            //因为HTTP1.1存在对头堵塞的问题，所以当现有连接没有超过MAX_CONNECTIONS_PER_URL时，都选择新建连接
            synchronized (channelArrayBlockingQueue) {
                if (channelArrayBlockingQueue.size() < MAX_CONNECTIONS_PER_URL)
                    createConnection(host, port, channelArrayBlockingQueue);
            }

            //获取一个连接，当连接被使用时，阻塞调用线程，实现了所谓“队头阻塞”
            channel = channelArrayBlockingQueue.poll();

            return channel;
        }
        ```

        链接创建过程如下：
        ```java
        /**
         * 创建连接并放入集合
        * @param host IP地址
        * @param port 端口
        * @param channelQueue 存放连接的集合
        * @return
        */
        private void createConnection(String host, int port, Queue<Channel> channelQueue) {
            ChannelFuture channelFuture = null;
            Channel result = null;
            try {
                channelFuture = bootstrap.connect(host,port).sync();
            } catch (InterruptedException e) {
                log.info("Connect to %s : %d failed: [%s]", host, port, e.getLocalizedMessage());
                result = null;
            }

            if (channelFuture != null) {
                channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        //关闭时移除这个连接
                        channelQueue.remove(future.channel());
                    }
                });
                result = channelFuture.channel();
                channelQueue.add(result);
            }
        }
        ```

        将netty的CHANNEL封装成NettyConnection暴露给外部

        ```java
        public class NettyConnection {
            private NettyConnectionPool connectionPool = NettyConnectionPool.getInstance();

            private String host;
            private int port;
            private Channel channel;

            public ChannelPromise sendMessageByHttp(FullHttpRequest request, String host, int port) throws InterruptedException {
                //每次都要重新获取连接，已实现队头阻塞
                this.channel = connectionPool.getOneChannel(host, port);
                this.host = host;
                this.port = port;
                ClientHandler handler = (ClientHandler)channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
                return handler.sendMessage(request);
            }

            public FullHttpResponse sendMessageByHttpSync(FullHttpRequest request, String host, int port) throws InterruptedException {
                //每次都要重新获取连接，已实现队头阻塞
                this.channel = connectionPool.getOneChannel(host, port);
                this.host = host;
                this.port = port;
                ClientHandler handler = (ClientHandler)channel.pipeline().get(NettyConnectionPool.CLIENT_HANDLER);
                ChannelPromise promise = handler.sendMessage(request);
                promise.await(30, TimeUnit.SECONDS);
                return handler.getResponse();
            }

            public void release() {
                if(channel != null) {
                    connectionPool.releaseChannel(host, port, channel);
                    channel = null;
                    host = null;
                }
            }

            public void close() {
                if(channel != null) {
                    channel.close();
                }
            }
        }

        ```

        由ClientHandler暴露同步或异步的读写

        ```java
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            response = msg;
            //增加引用计数，以防止被释放
            ReferenceCountUtil.retain(response);
            promise.setSuccess();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            this.ctx = ctx;
            //标记此通道已连接
            connected.countDown();
        }

        /**
        * 发送请求，默认超时时间是30S，目前先写死这个时间
        * @param request 请求
        * @return 一个ChannelPromise，用于获取结果
        * @throws InterruptedException
        */
        public ChannelPromise sendMessage(FullHttpRequest request) throws InterruptedException {
            connected.await(30, TimeUnit.SECONDS);
            promise = ctx.newPromise();
            ctx.writeAndFlush(request);
            return promise;
        }

        public FullHttpResponse getResponse() {
            return response;
        }
        ```

- 第二次作业

  使用内部账户BGL作为转账处理，这样confirm不需要有实际性动作，cancel也很简单
  ```java
    @Slf4j
    @Service
    @DubboService(retries = 0)
    public class TransferServiceImpl implements TransferService {

        @Autowired(required = false)
        private AccountMapper accountMapper;

        @Override
        @Transactional
        @HmilyTCC(confirmMethod = "confirm", cancelMethod = "cancel")
        public boolean transferMoney(Account account) {
            log.info("transfering to BGL...");

            boolean from = accountMapper.transferOut(account) > 0;
            boolean to = accountMapper.transferIn2Bgl(account) > 0;

            return from && to;
        }

        /**
        * 因为在try步已经把资金转入了内部账户，所以此时什么也不需要做
        * @param account
        * @return
        */
        public boolean confirm(Account account) {
            return true;
        }

        public boolean cancel(Account account) {
            log.info("begin to inverse...");
            //将金额取负数
            account.setAmount(-account.getAmount());
            boolean from = accountMapper.transferOut(account) > 0;
            boolean to = accountMapper.transferIn2Bgl(account) > 0;

            return from && to;
        }
    }
  ```
