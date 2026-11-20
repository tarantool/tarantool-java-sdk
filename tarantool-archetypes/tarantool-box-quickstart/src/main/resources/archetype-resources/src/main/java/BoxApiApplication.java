package ${package};

import io.tarantool.driver.api.TarantoolBoxClient;
import io.tarantool.driver.api.TarantoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class BoxApiApplication {
    private static final Logger log = LoggerFactory.getLogger(BoxApiApplication.class);

    public static void main(String[] args) {
        // 1. Initialize the Box API client.
        // For local development without authentication:
        TarantoolBoxClient client = TarantoolFactory.box()
                .withHost("localhost")
                .withPort(3301)
                // .withCredentials("guest", "") // Uncomment if explicit authentication is required
                .build();

        try {
            // 2. Prepare the schema (idempotent).
            // IMPORTANT: in production, manage the schema via migrations
            // (Flyway, Tarantool migrations), not via eval on application startup.
            client.eval("return box.schema.create_space('person', {if_not_exists = true})").join();
            client.eval("return box.space.person:create_index('pk', " +
                    "{parts = {1, 'unsigned'}, if_not_exists = true})").join();

            // 3. Work with data through the low-level Box API
            var space = client.space("person");
            space.insert(Arrays.asList(1L, "Alice")).join();

            var result = space.select(1L).join();
            log.info("Retrieved tuple: {}", result.get(0));

        } finally {
            client.close();
        }
    }
}
