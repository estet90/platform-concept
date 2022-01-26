package ru.craftysoft.platform.gateway;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import ru.craftysoft.platform.gateway.configuration.MainVerticle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;

@ApplicationScoped
public class Starter {

    private final Vertx vertx;
    private final MainVerticle mainVerticle;

    public Starter(Vertx vertx, @Named("mainVerticle") MainVerticle mainVerticle) {
        this.vertx = vertx;
        this.mainVerticle = mainVerticle;
    }

    public void start(@Observes StartupEvent event) {
        vertx.deployVerticle(mainVerticle);
    }

    public void stop(@Observes ShutdownEvent event) {
        vertx.close(asyncResult -> {
            if (asyncResult.failed()) {
                asyncResult.cause().printStackTrace();
            } else {
                System.out.println("Сервер остановлен");
            }
        });
    }

}
