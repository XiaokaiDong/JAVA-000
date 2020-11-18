import beans.annotation.StudentConf;
import beans.domain.CollegeStuedent;
import beans.domain.Klass;
import beans.domain.Student;
import beans.factory.IContextFactory;
import beans.factory.Impl.AnnotationContextFactory;
import beans.factory.Impl.XmlContextFactory;
import beans.factory.util.ContextUtil;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.xml.bind.annotation.XmlAccessOrder;
import java.util.Map;

public class Lookup {
    public static void main(String[] args) {
        //使用XML配置文件按照类型查找
        xmlLookupCollectionByType();

        //使用注解按照类型查找
        annoLookupCollectionByType();

        //创建一个应用上下文
        IContextFactory annoContextFactory = new AnnotationContextFactory<StudentConf>(StudentConf.class);

        AnnotationConfigApplicationContext applicationContext = (AnnotationConfigApplicationContext)annoContextFactory.getFactory();;
        applicationContext.refresh();

        //使用名字进行查找
        lookupByBeanByName(applicationContext);

        //使用ObjectFactory进行延迟查找
        lookupByStudentFactory();

        //使用ObjectProvider进行延迟查找
        lookupByStudentProvider(applicationContext);

        //------------------------------------

        //从AnnotationConfigApplicationContext得到ConfigurableListableBeanFactory
        //即ApplicationContext组合了ConfigurableListableBeanFactory，而接口ConfigurableListableBeanFactory的关系如下
        //HierarchicalBeanFactory <- ConfigurableBeanFactory <- ConfigurableListableBeanFactory
        ConfigurableListableBeanFactory configurableListableBeanFactory = applicationContext.getBeanFactory();

        //查看当前BeanFactory的父BeanFactory
        System.out.println("当前 BeanFactory 的 Parent BeanFactory :" +
                configurableListableBeanFactory.getParentBeanFactory());
        //设置Parent BeanFactory
        configurableListableBeanFactory
                .setParentBeanFactory(new XmlContextFactory("classpath:/applicationContext.xml").getFactory());

        System.out.println(configurableListableBeanFactory.containsLocalBean("student100"));

        //------------------------------------

        System.out.println("The class1 is: " + applicationContext.getBean("class1"));
        //下面的依赖查找只能在某一级的上下文中寻找，无法将父子上下文中的BEAN都找到
        System.out.println("The class2 is: " + applicationContext.getBean("class2"));

        //关闭应用上下文
        applicationContext.close();
    }

    //使用XML配置文件按照类型查找
    private static void xmlLookupCollectionByType(){

        IContextFactory contextFactory= new XmlContextFactory("classpath:/applicationContext.xml");
        Map<String, Student> students = ContextUtil.lookupCollectionByType(contextFactory.getFactory(), Student.class);
        System.out.println("查找到的所有的 User 集合对象：" + students);
        System.out.println("----------------------------------------");
    }

    //使用注解按照类型查找
    private static void annoLookupCollectionByType(){
        IContextFactory annoContextFactory = new AnnotationContextFactory<StudentConf>(StudentConf.class);
        BeanFactory beanFactory = annoContextFactory.getFactory();

        AnnotationConfigApplicationContext applicationContext = (AnnotationConfigApplicationContext)beanFactory;
        applicationContext.refresh();
        Map<String, Student> students1 = ContextUtil.lookupCollectionByType(applicationContext, Student.class);
        System.out.println("查找到的所有的 User 集合对象：" + students1);
        System.out.println("----------------------------------------");
    }

    //使用名字进行查找
    private static void lookupByBeanByName(AnnotationConfigApplicationContext applicationContext){
        Student student = (Student) ContextUtil.lookupByBeanByName(applicationContext, "student");
        System.out.println("使用名字进行查找到的student：" + student);
        System.out.println("----------------------------------------");
    }

    //使用ObjectFactory进行延迟查找
    private static void lookupByStudentFactory(){
        IContextFactory contextFactory= new XmlContextFactory("classpath:/applicationContext.xml");
        Student student123 = ContextUtil.lookupByStudentFactory(contextFactory.getFactory()).getObject();
        System.out.println("使用ObjectFactory查找到的student：" + student123);
        System.out.println("----------------------------------------");
    }

    //使用ObjectProvider进行延迟查找
    private static void lookupByStudentProvider(AnnotationConfigApplicationContext applicationContext){
        ObjectProvider<Student> studentObjectProvider = ContextUtil.lookupByStudentProvider(applicationContext);

        Student student = null;

        student = studentObjectProvider.getObject();
        System.out.println("使用ObjectProvider.getObject进行延迟查找到的student：" + student);
        System.out.println("----------------------------------------");

        CollegeStuedent collegeStuedent = (CollegeStuedent) studentObjectProvider.getIfAvailable(
                () -> CollegeStuedent.create(1, "tt", "geek" ));
        System.out.println("使用ObjectProvider.getIfAvailable进行延迟查找到的collegeStuedent：" + collegeStuedent);
        System.out.println("----------------------------------------");
    }
}
