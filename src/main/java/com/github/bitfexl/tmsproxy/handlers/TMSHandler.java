package com.github.bitfexl.tmsproxy.handlers;

import com.github.bitfexl.tmsproxy.config.Config;
import com.github.bitfexl.tmsproxy.config.TileSource;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class TMSHandler implements Handler<RoutingContext> {
    private static final String DOT_PATTERN = Pattern.quote(".");

    private final HttpClient httpClient;

    private final Config config;

    public TMSHandler(Router router, Vertx vertx, Config config) {
        router.route(HttpMethod.GET, "/tms/:name/:z/:x/:y").handler(this);

        httpClient = vertx.createHttpClient();
        this.config = config;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            final String name = ctx.pathParam("name");
            final int z = Integer.parseInt(ctx.pathParam("z"));
            final int x = Integer.parseInt(ctx.pathParam("x"));
            final int y = Integer.parseInt(ctx.pathParam("y").split(DOT_PATTERN)[0]);
            handleTmsRequest(ctx, name, z, x, y);
        } catch (NumberFormatException ex) {
            ctx.next();
        }
    }

    private void handleTmsRequest(RoutingContext ctx, String name, int z, int x, int y) {
        final TileSource layer = config.getTileSources().get(name);

        if (layer == null) {
            ctx.next();
            return;
        }

        final String url = layer.buildUrl(z, x, y);

        httpClient.request(new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url))
                .compose(HttpClientRequest::send)
                .onSuccess(response -> {
                    ctx.response().setStatusCode(response.statusCode());
                    ctx.response().putHeader("Content-Type", response.getHeader("Content-Type"));
                    ctx.response().putHeader("Content-Length", response.getHeader("Content-Length"));
                    response.pipeTo(ctx.response());
                })
                .onFailure(t -> {
                    log.error("Error forwarding request.", t);
                    ctx.response().setStatusCode(400).end();
                });
    }

    private String getPathParams(RoutingContext ctx) {
        final StringBuilder params = new StringBuilder();

        for (String param : ctx.pathParams().keySet().stream().sorted().toList()) {
            params.append(param)
                    .append(" = ")
                    .append(ctx.pathParam(param))
                    .append("\n");
        }

        return params.toString();
    }
}
