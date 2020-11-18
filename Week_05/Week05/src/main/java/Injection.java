import beans.config.JavaCompusClassConfig;
import beans.domain.JavaCompusClass;
import beans.domain.JavaCompusGeektime;
import beans.domain.Klass;
import beans.domain.Student;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.applet.AppletContext;
import java.util.List;

public class Injection {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
        xmlSetterInjection(applicationContext, "class1");
        xmlSetterInjection(applicationContext, "classAutowireByType");
        xmlSetterInjection(applicationContext, "classAutowireByName");
        xmlSetterInjection(applicationContext, "classWithListSetup");

        annoSetterInjection("classBySetterInjection");

        apiSetterInjection();

        xmlCnstrctrArgInjection(applicationContext);
        annoCnstrctrArgInjection();

        autowiredInjection();
        methodInjection();
    }

    //xml setter注入
    private static void xmlSetterInjection(ApplicationContext applicationContext, String beanName) {
        //直接依赖XML中的配置
        Klass class1 = applicationContext.getBean(beanName, Klass.class);
        System.out.println("Injection by setter in xmlSetterInjection, the bean[" + beanName + "] is: " + class1);
    }

    //annotation setter注入
    private static void annoSetterInjection(String beanName){
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(Injection.class);
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(annotationConfigApplicationContext);
        String xmlResourcePath = "classpath:/applicationContext.xml";
        beanDefinitionReader.loadBeanDefinitions(xmlResourcePath);
        annotationConfigApplicationContext.refresh();

        Klass class2 = annotationConfigApplicationContext.getBean(beanName, Klass.class);
        System.out.println("Injection by setter in annoSetterInjection, the result is: " + class2);

        annotationConfigApplicationContext.close();
    }

    //使用API的方式注入
    private static void apiSetterInjection(){
        //需要一个实现了BeanDefinitionRegistry的上下文
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();

        //此处不需要注册配置类了，因为是直接通过API加载的
        //annotationConfigApplicationContext.register(Injection.class);

        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(annotationConfigApplicationContext);
        String xmlResourcePath = "classpath:/applicationContext.xml";
        beanDefinitionReader.loadBeanDefinitions(xmlResourcePath);
        BeanDefinition klassBeanDefinition = createKlassBeanDefinition();
        annotationConfigApplicationContext.registerBeanDefinition("aipClass", klassBeanDefinition);
        annotationConfigApplicationContext.refresh();

        Klass class1 = annotationConfigApplicationContext.getBean("aipClass",Klass.class);
        System.out.println("Injection by api setter in apiSetterInjection, the result is: " + class1);

        annotationConfigApplicationContext.close();
    }

    private static BeanDefinition createKlassBeanDefinition() {
        BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(Klass.class);
        definitionBuilder.addPropertyReference("students", "studentList");
        return definitionBuilder.getBeanDefinition();
    }

    //下面的会自动将上下文里可赋给Student类型的bean放入列表中，参见
    //https://blog.csdn.net/nlznlz/article/details/82528411
    @Bean
    @Qualifier   //即使加上这个限定也不行
    public Klass classBySetterInjection(List<Student> students){
        Klass class1 = new Klass();
        class1.setStudents(students);
        return class1;
    }

    //xml 构造器参数方式注入
    private static void xmlCnstrctrArgInjection(ApplicationContext applicationContext){
        //直接依赖XML中的配置
        Klass class1 = applicationContext.getBean("xmlClassInjectedByCnstrctrArg", Klass.class);
        System.out.println("Injection by xml-constructor-arg, the result is: " + class1);

        //自动注入是按照类型进行的，无法精确控制注入的BEAN
        class1 = applicationContext.getBean("xmlClassInjectedByAutowiredCnstrctrArg", Klass.class);
        System.out.println("Injection by xml-autowired-constructor-arg, the result is: " + class1);
    }

    //注解构造器参数方式注入
    private static void annoCnstrctrArgInjection(){
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(Injection.class);
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(annotationConfigApplicationContext);
        String xmlResourcePath = "classpath:/applicationContext.xml";
        beanDefinitionReader.loadBeanDefinitions(xmlResourcePath);
        annotationConfigApplicationContext.refresh();

        //直接依赖XML中的配置
        Klass class1 = annotationConfigApplicationContext.getBean("classBySetterInjection", Klass.class);
        System.out.println("Injection by annotation-constructor-arg, the classBySetterInjection is: " + class1);

        annotationConfigApplicationContext.close();
    }

    @Bean
    public Klass classByConstructorInjection(List<Student> students){
        return new Klass(students);
    }

    @Bean
    @Qualifier
    public Student qualifiedStudent1(){
        return new Student(111, "qualifiedStudent1");
    }

    @Bean
    @Qualifier
    public Student qualifiedStudent2(){
        return new Student(222, "qualifiedStudent2");
    }

    private static void autowiredInjection(){
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(JavaCompusClassConfig.class);
        annotationConfigApplicationContext.refresh();

        JavaCompusClass javaCompusClass = annotationConfigApplicationContext.getBean("javaCompusClass", JavaCompusClass.class);
        System.out.println("Injection by @Autowired, the result is: " + javaCompusClass);


        annotationConfigApplicationContext.close();
    }

    private static void methodInjection(){
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(JavaCompusClassConfig.class);
        annotationConfigApplicationContext.refresh();

        JavaCompusGeektime javaCompusGeektime = annotationConfigApplicationContext.getBean("javaCompusGeektime", JavaCompusGeektime.class);
        System.out.println("Injection by method injection, the result is: " + javaCompusGeektime);


        annotationConfigApplicationContext.close();
    }
}
