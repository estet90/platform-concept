package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import lombok.SneakyThrows;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ApplicationScoped
public class GraphQlConfiguration {

    @ApplicationScoped
    public GraphQL graphQl(MainResolver mainResolver) {
        var contract = parse();
        return GraphQlFactory.graphQl(mainResolver::resolve, contract);
    }

    @SneakyThrows
    private String parse() {
        var graphqlAsBytes = Objects.requireNonNull(GraphQlConfiguration.class.getResourceAsStream("/gateway.graphqls"))
                .readAllBytes();
        return new String(graphqlAsBytes, StandardCharsets.UTF_8);
    }
}
