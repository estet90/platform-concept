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

    private final Vertx applicationVertx;
    private final MainVerticle mainVerticle;

    public Starter(@Named("applicationVertx") Vertx applicationVertx,
                   @Named("mainVerticle") MainVerticle mainVerticle) {
        this.applicationVertx = applicationVertx;
        this.mainVerticle = mainVerticle;
    }

    public void start(@Observes StartupEvent event) {
        applicationVertx.deployVerticle(mainVerticle);
//        applicationVertx.deployVerticle(MainVerticle.class, new DeploymentOptions().setInstances(4), new Handler<AsyncResult<String>>() {
//            @Override
//            public void handle(AsyncResult<String> event) {
//
//            }
//        });
    }

    public void stop(@Observes ShutdownEvent event) {
        applicationVertx.close(asyncResult -> {
            if (asyncResult.failed()) {
                asyncResult.cause().printStackTrace();
            } else {
                System.out.println("Сервер остановлен");
            }
        });
    }

}
