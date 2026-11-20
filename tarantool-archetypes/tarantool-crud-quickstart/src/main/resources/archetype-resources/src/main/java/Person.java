package ${package};

import org.springframework.data.annotation.Id;
import org.springframework.data.tarantool.core.mapping.Field;
import org.springframework.data.tarantool.core.mapping.Tuple;

/**
 * Entity mapped to the Tarantool {@code person} space.
 * {@link Tuple} binds the class to the space name,
 * {@link Field} maps Java fields to tuple fields,
 * and {@link Id} marks the primary key field used by findById/save.
 */
@Tuple("person")
public class Person {

    @Id
    @Field(name = "id")
    private Long id;

    @Field(name = "name")
    private String name;

    public Person() {
    }

    public Person(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Person{id=" + id + ", name='" + name + "'}";
    }
}
