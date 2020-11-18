package beans.domain;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

@Data
@ToString
public class JavaCompusGeektime {
    private Teacher assistantTeacher;
    private Teacher managementTeacher;
    private Teacher managementTeacher2;

    @Autowired
    @Qualifier("assistantTeacher")
    public void initAssistantTeacher(Teacher assistantTeacher){
        this.assistantTeacher = assistantTeacher;
    }

    @Autowired
    @Qualifier("managementTeacher")
    public void initManagementTeacher(Teacher managementTeacher){
        this.managementTeacher = managementTeacher;
    }

    //下面这个方法没有生效，不知道为啥！！！
    @Bean(name = "managementTeacher2")
    public void getManagementTeacher(Teacher managementTeacher) {
        this.managementTeacher2 = managementTeacher;
    }
}
