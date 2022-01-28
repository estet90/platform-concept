package ru.craftysoft.platform.gateway.configuration.property;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "graphql")
public interface GraphQlServersByMethodsMap {

    Map<String, String> serversByMethods();

}
