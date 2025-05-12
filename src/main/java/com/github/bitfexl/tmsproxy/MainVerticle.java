package com.github.bitfexl.tmsproxy;

import com.github.bitfexl.tmsproxy.config.Config;
import com.github.bitfexl.tmsproxy.config.ConfigParser;
import com.github.bitfexl.tmsproxy.handlers.DefaultHandler;
import com.github.bitfexl.tmsproxy.handlers.TMSHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class MainVerticle extends AbstractVerticle {
    private final Config config;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        final HttpServer httpServer = vertx.createHttpServer();
        final Router router = Router.router(vertx);
        registerRoutes(router);
        httpServer.requestHandler(router);
        httpServer.listen(config.getPort(), "127.0.0.1")
                .onComplete(event -> {
                    if (event.succeeded()) {
                        startPromise.complete();
                        log.info("HTTP server listening on port {}.", httpServer.actualPort());
                    } else {
                        startPromise.fail(event.cause());
                    }
                });
    }

    private void registerRoutes(Router router) {
        new TMSHandler(router, vertx, config);
        new DefaultHandler(router);
    }

    public static void main(String[] args) {
        final long startTime = System.currentTimeMillis();

        String configFile = "tmsconfig.json";
        if (args.length > 0) {
            configFile = args[0];
        }
        Config config;
        try {
            config = readConfig(configFile);
        } catch (Exception ex) {
            log.error("Error loading config file '{}'.", configFile, ex);
            System.exit(1);
            return;
        }

        Vertx.vertx().deployVerticle(new MainVerticle(config))
                .onComplete(event -> {
                    long endTime = System.currentTimeMillis();
                    if (event.succeeded()) {
                        log.info("HTTP server started in {}ms.", (endTime - startTime));
                    } else {
                        log.error("HTTP server startup failed after {}ms.", (endTime - startTime), event.cause());
                    }
                });
    }

    @SneakyThrows
    public static Config readConfig(String configFile) {
        try (final InputStream in = new FileInputStream(configFile)) {
            final String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new ConfigParser().parseConfig((JsonObject)Json.decodeValue(config));
        }
    }
}
