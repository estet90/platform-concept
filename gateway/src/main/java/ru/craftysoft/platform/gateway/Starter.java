package ru.craftysoft.platform.gateway;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.AbstractVerticle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;

@ApplicationScoped
public class Starter {

    private final AbstractVerticle mainVerticle;

    public Starter(@Named("mainVerticle") AbstractVerticle mainVerticle) {
        this.mainVerticle = mainVerticle;
    }

    public void init(@Observes StartupEvent event) throws Exception {
        mainVerticle.start();
    }

}
