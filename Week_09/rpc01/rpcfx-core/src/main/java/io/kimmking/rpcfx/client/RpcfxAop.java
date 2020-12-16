package io.kimmking.rpcfx.client;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
