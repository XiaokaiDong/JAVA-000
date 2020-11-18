package beans.config;

import beans.domain.JavaCompusClass;
import beans.domain.JavaCompusGeektime;
import beans.domain.Student;
import beans.domain.Teacher;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.school", name = "enabled", havingValue = "true", matchIfMissing = false)
public class JavaCompusClassConfig {
    @Bean
    public Teacher javaTeacher(){
        return new Teacher("Java", "KK");
    }

    @Bean
    public Teacher assistantTeacher() {
        return new Teacher("assistant", "Cuicui");
    }

    @Bean
    public Teacher managementTeacher() {
        return new Teacher("management", "Jiajia");
    }

    @Bean
    public Teacher managementTeacher2() {
        return new Teacher("management", "Shuangshuang");
    }

    @Bean
    public Student javaStudent1() {
        return new Student(789, "student_in_class_#1");
    }

    @Bean
    public Student javaStudent5() {
        return new Student(135, "student_in_class_#5");
    }

    @Bean
    public JavaCompusClass javaCompusClass() {
        return new JavaCompusClass();
    }

    @Bean
    public JavaCompusGeektime javaCompusGeektime() {
        return new JavaCompusGeektime();
    }
}
