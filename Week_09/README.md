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

        客户端主程序如下：

        ```java
        public class NettyHttpClient {

            //线程组
            private final EventLoopGroup group = new NioEventLoopGroup();

            //启动类
            private final Bootstrap bootstrap = new Bootstrap();

            //目标URL
            private URL url;

            private ChannelFuture channelFuture;

            public void start(URL url) throws InterruptedException {
                this.url = url;
                try {
                    String host = url.getHost();
                    int port = url.getPort();
                    bootstrap.group(group)
                            .remoteAddress(new InetSocketAddress(host, port))
                            //长连接
                            .option(ChannelOption.SO_KEEPALIVE, true)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<Channel>() {
                                protected void initChannel(Channel channel) throws Exception {

                                    //包含编码器和解码器
                                    channel.pipeline().addLast(new HttpClientCodec());

                                    //聚合
                                    channel.pipeline().addLast(new HttpObjectAggregator(32 * 1024));

                                    //解压
                                    channel.pipeline().addLast(new HttpContentDecompressor());

                                    //channel.pipeline().addLast(new ClientHandler());
                                }
                            });

                    channelFuture = bootstrap.connect().sync();

                    channelFuture.channel().closeFuture().sync();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    group.shutdownGracefully();
                }
            }

            public RpcfxResponse post(RpcfxRequest req){
                String reqJson = JSON.toJSONString(req);
                System.out.println("req json: "+reqJson);

                FullHttpRequest request = null;
                try {
                    request = new DefaultFullHttpRequest(
                            HttpVersion.HTTP_1_1, HttpMethod.POST,
                            url.toString(),
                            Unpooled.wrappedBuffer(reqJson.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                request.headers()
                        .set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
                        //开启长连接
                        .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                        //设置传递请求内容的长度
                        .set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());

                try {
                    channelFuture.channel().write(request).sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
        ```

