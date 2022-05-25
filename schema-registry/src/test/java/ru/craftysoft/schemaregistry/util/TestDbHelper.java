package ru.craftysoft.schemaregistry.util;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class TestDbHelper {

    @SneakyThrows
    public static void executeLiquibaseMigrations(Supplier<Connection> connectionFactory, String path, String defaultSchemaName) {
        try (var connection = connectionFactory.get()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(defaultSchemaName);
            executeLiquibaseMigrations(path, new ClassLoaderResourceAccessor(), database);
        }
    }

    @SneakyThrows
    public static void executeLiquibaseMigrations(String path, ClassLoaderResourceAccessor classLoaderResourceAccessor, Database database) {
        try (var liquibase = new Liquibase(path, classLoaderResourceAccessor, database)) {
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }

    @SneakyThrows
    public static void executeQueryFromClasspath(Supplier<Connection> connectionFactory, String path) {
        try (var inputStream = TestDbHelper.class.getClassLoader().getResourceAsStream(path);
             var reader = new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8);
             var bufferedReader = new BufferedReader(reader)) {
            var sql = bufferedReader.lines()
                    .collect(Collectors.joining("\n"));
            try (var connection = connectionFactory.get();
                 var preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.execute();
            }
        }
    }

    @SneakyThrows
    public static Connection getConnection(String url, String username, String password) {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(url, username, password);
    }

}
