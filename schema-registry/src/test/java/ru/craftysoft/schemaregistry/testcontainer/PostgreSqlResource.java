package ru.craftysoft.schemaregistry.testcontainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.util.Map;
import java.util.function.Supplier;

import static ru.craftysoft.schemaregistry.util.TestDbHelper.*;

public class PostgreSqlResource implements QuarkusTestResourceLifecycleManager {

    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.3-alpine")
            .withDatabaseName("schema_registry");

    @Override
    public Map<String, String> start() {
        postgres.start();
        var jdbcUrl = postgres.getJdbcUrl();
        var username = postgres.getUsername();
        var password = postgres.getPassword();
        Supplier<Connection> connectionFactory = () -> getConnection(jdbcUrl, username, password);
        executeQueryFromClasspath(connectionFactory, "db/setup/3_create_schema.sql");
        executeLiquibaseMigrations(connectionFactory, "db/migration/changelog.xml", "schema_registry");
        return Map.of(
                "quarkus.datasource.reactive.url", jdbcUrl.substring("jdbc:".length()),
                "quarkus.datasource.jdbc.url", jdbcUrl,
                "quarkus.datasource.username", username,
                "quarkus.datasource.password", password
        );
    }

    @Override
    public void stop() {
        postgres.stop();
    }

}
