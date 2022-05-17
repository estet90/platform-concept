package ru.craftysoft.platform.gateway.configuration;

import graphql.GraphQL;
import ru.craftysoft.platform.gateway.configuration.property.GraphQlServicesByMethodsMap;
import ru.craftysoft.platform.gateway.resolver.MainResolver;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GraphQlConfiguration {

    @ApplicationScoped
    public GraphQL graphQl(MainResolver mainResolver, GraphQlServicesByMethodsMap graphQlServersByMethods) {
        return GraphQlFactory.graphQlFromPaths(mainResolver::resolve, graphQlServersByMethods.contractsByServices().values());
    }

}
