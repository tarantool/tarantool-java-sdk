package ${package};

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.tarantool.repository.config.EnableTarantoolRepositories;

@SpringBootApplication
@EnableTarantoolRepositories
public class CrudApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrudApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(PersonRepository repository) {
        return args -> {
            Person saved = repository.save(new Person(1L, "Bob"));
            System.out.println("Saved: " + saved);

            repository.findById(1L).ifPresent(p ->
                    System.out.println("Found: " + p));
        };
    }
}
