package beans.factory.util;

import beans.domain.Student;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

public class ContextUtil {
    //按类型查找
    public static <T> Map<String, T> lookupCollectionByType(BeanFactory beanFactory, Class<T> clazz) {
        if(beanFactory instanceof ListableBeanFactory){
            ListableBeanFactory listableBeanFactory = (ListableBeanFactory)beanFactory;
            return listableBeanFactory.getBeansOfType(clazz);
        }
        return null;
    }

    //按名字查找
    public static Object lookupByBeanByName(BeanFactory beanFactory, String beanName) {
        return beanFactory.getBean(beanName);
    }

    //使用ObjectFactory进行延迟查找
    public static ObjectFactory<Student> lookupByStudentFactory(BeanFactory beanFactory) {
        return (ObjectFactory<Student>) beanFactory.getBean("studentFactory");
    }

    //使用ObjectProvider进行延迟查找
    public static ObjectProvider<Student> lookupByStudentProvider(AnnotationConfigApplicationContext applicationContext){
        return applicationContext.getBeanProvider(Student.class);
    }

}
