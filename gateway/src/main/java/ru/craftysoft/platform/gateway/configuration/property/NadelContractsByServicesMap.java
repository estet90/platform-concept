package ru.craftysoft.platform.gateway.configuration.property;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "nadel")
public interface NadelContractsByServicesMap {

    Map<String, String> graphqlContractsByServices();

    Map<String, String> nadelContractsByServices();

}
