package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ApplicationScoped
public class GraphQlConfiguration {

    @ApplicationScoped
    @Named("graphQl")
    public GraphQL graphQl(MainResolver mainResolver) {
        var contract = parse();
        return GraphQlFactory.graphQl(mainResolver::resolve, contract);
    }

    private String parse() {
        try {
            var graphqlAsBytes = Objects.requireNonNull(GraphQlConfiguration.class.getResourceAsStream("/gateway.graphqls"))
                    .readAllBytes();
            return new String(graphqlAsBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
