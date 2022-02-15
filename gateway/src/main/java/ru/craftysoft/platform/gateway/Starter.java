package ru.craftysoft.platform.gateway;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.craftysoft.platform.gateway.configuration.MainVerticle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
@RequiredArgsConstructor
public class Starter {

    private final Vertx vertx;
    private final MainVerticle mainVerticle;

    private static final Logger logger = LoggerFactory.getLogger(Starter.class);

    public void start(@Observes StartupEvent event) {
        vertx.deployVerticle(mainVerticle);
    }

    public void stop(@Observes ShutdownEvent event) {
        vertx.close(asyncResult -> {
            if (asyncResult.failed()) {
                logger.error("Ошибки при остановке сервера: ", asyncResult.cause());
            } else {
                logger.info("Сервер успешно остановлен");
            }
        });
    }

}
