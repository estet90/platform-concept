package ru.craftysoft.platform.gateway.configuration;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RequiredArgsConstructor
public class MainVerticle extends AbstractVerticle {

    private final Router router;

    private HttpServer server;

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() {
        server = vertx.createHttpServer()
                .requestHandler(router);
        server.listen(8080)
                .onComplete(httpServerAsyncResult -> {
                    if (httpServerAsyncResult.failed()) {
                        logger.error("Ошибка при запуске: ", httpServerAsyncResult.cause());
                    }
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        server.close(stopPromise);
    }

}
