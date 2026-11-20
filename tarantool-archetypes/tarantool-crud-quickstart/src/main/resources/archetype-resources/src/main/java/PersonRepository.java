package ${package};

import org.springframework.data.repository.CrudRepository;

/**
 * Repository interface for the {@link Person} entity.
 * By extending {@link CrudRepository}, it automatically inherits implementations
 * of the standard CRUD operations: save(), findById(), delete(), findAll(), etc.
 * No implementation class is required — Spring Data generates a proxy at runtime.
 */
public interface PersonRepository extends CrudRepository<Person, Long> {
}
