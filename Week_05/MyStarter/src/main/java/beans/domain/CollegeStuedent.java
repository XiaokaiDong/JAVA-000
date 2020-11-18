package beans.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class CollegeStuedent extends Student{
    private String collegeName;

    public CollegeStuedent(int id, String name, String collegeName) {
        super(id, name);
        this.collegeName = collegeName;
    }

    public static CollegeStuedent create(int id, String name, String collegeName){
        CollegeStuedent result = new CollegeStuedent(id, name, collegeName);
        return result;
    }
}
