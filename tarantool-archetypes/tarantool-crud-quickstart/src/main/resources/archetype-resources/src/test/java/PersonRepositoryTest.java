package ${package};

import io.tarantool.testcontainers.TarantoolCartridgeContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class PersonRepositoryTest {

    @Container
    // TarantoolCartridgeContainer starts a Cartridge cluster with the
    // vshard and crud roles enabled, which is required for CrudRepository
    static final TarantoolCartridgeContainer tarantool =
            new TarantoolCartridgeContainer("instances.yml", "topology.lua")
                    .withRouterHost("localhost")
                    .withRouterPort(3301)
                    .withRouterUsername("admin")
                    .withRouterPassword("secret-cluster-cookie");

    @Autowired
    private PersonRepository personRepository;

    @Test
    void shouldSaveAndFindPerson() {
        Person person = new Person(1L, "Alice");
        Person saved = personRepository.save(person);
        assertNotNull(saved);

        assertTrue(personRepository.findById(1L).isPresent());
    }
}
