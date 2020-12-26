package io.kimmking.rpcfx.client.aop.annotation;

public @interface RpcfxService {
    String url();
    String group() default "";
    String version() default "";
    String registry() default "";
}
