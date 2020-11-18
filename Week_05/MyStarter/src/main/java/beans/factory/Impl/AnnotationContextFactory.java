package beans.factory.Impl;

import beans.factory.IContextFactory;
import beans.factory.IInitializeable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.Closeable;

public class AnnotationContextFactory <T> implements IContextFactory {
    private AnnotationConfigApplicationContext applicationContext;
    private Class<T> confClazz;

    public AnnotationContextFactory(Class<T> confClazz) {
        this.confClazz = confClazz;
        applicationContext = null;
    }


    @Override
    public BeanFactory getFactory() {
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(confClazz);

        return applicationContext;
    }


}
