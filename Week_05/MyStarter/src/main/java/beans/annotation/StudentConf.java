package beans.annotation;

import beans.domain.CollegeStuedent;
import beans.domain.Student;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StudentConf {
    @Bean
    public Student student(){
        Student student = new Student();
        student.setId(1);
        student.setName("tt");
        return student;
    }

    @Bean
    @Primary
    public CollegeStuedent collegeStuedent(){
        CollegeStuedent collegeStuedent = new CollegeStuedent();
        collegeStuedent.setId(2);
        collegeStuedent.setName("collegeStuedent");
        collegeStuedent.setName("geek");
        return collegeStuedent;
    }
}
