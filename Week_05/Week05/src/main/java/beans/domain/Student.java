package beans.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Student implements Serializable {

    private int id;
    private String name;

    public void init(){
        System.out.println("hello...........");
    }

    public static Student create(){
        return new Student(101,"KK101");
    }

    public static Student create(int id, String name){
        return new Student(id, name);
    }
}