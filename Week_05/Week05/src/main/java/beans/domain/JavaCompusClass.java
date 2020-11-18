package beans.domain;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

@Data
@ToString
public class JavaCompusClass {
    @Autowired
    @Qualifier("javaTeacher")
    private Teacher teacher;

    @Autowired
    @Qualifier("javaStudent1")
    private Student student1;

    @Resource
    @Qualifier("javaStudent5")
    private Student student5;
}
