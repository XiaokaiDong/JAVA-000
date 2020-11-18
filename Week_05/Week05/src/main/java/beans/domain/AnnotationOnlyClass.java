package beans.domain;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class AnnotationOnlyClass {
    @Autowired
    List<Student> students;
}
