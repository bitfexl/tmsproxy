package com.github.bitfexl.tmsproxy.handlers;

import com.github.bitfexl.tmsproxy.config.Config;
import com.github.bitfexl.tmsproxy.data.TileCache;
import com.github.bitfexl.tmsproxy.data.TileSource;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
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
        final TileSource tileSource = config.getTileSources().get(name);

        if (tileSource == null) {
            ctx.next();
            return;
        }

        if (z < tileSource.getMinZoom() || z > tileSource.getMaxZoom()) {
            ctx.next();
            return;
        }

        final TileCache tileCache;

        if (tileSource.getCache() != null) {
            tileCache = config.getTileCaches().get(tileSource.getCache());
            if (tileCache == null) {
                log.error("Tile cache configuration '{}' referenced by tile source '{}' is missing.", tileSource.getCache(), tileSource.getName());
            }
        } else {
            tileCache = null;
        }

        if (tileCache != null) {
            tileCache.retrieve(name, z, x, y).onComplete(event -> {
                if (event.succeeded() && !event.result().isEmpty()) {
                    ctx.response().putHeader("Content-Type", "image/" + event.result().extension());
                    if (event.result().fileContents() != null) {
                        ctx.response().end(event.result().fileContents());
                    } else {
                        ctx.response().sendFile(event.result().filePath());
                    }
                } else {
                    requestAndCache(ctx, name, z, x, y, tileSource, tileCache);
                }
            });
        } else {
            requestAndCache(ctx, name, z, x, y, tileSource, null);
        }
    }

    private void requestAndCache(RoutingContext ctx, String name, int z, int x, int y, TileSource tileSource, TileCache tileCache) {
        final HttpServerResponse response = ctx.response();
        final String url = tileSource.buildUrl(z, x, y);

        httpClient.request(new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url))
                .compose(HttpClientRequest::send)
                .onSuccess(upstreamResponse -> {
                    final String contentType = upstreamResponse.getHeader("Content-Type");

                    if (upstreamResponse.statusCode() < 200 || upstreamResponse.statusCode() > 299) {
                        if (upstreamResponse.statusCode() == 404) {
                            // will probably be a common "error", handle with next route or 404
                            ctx.next();
                            return;
                        }

                        // todo: retry
                        log.warn("Upstream server responded with status code '{} {}' for '{}'.", upstreamResponse.statusCode(), upstreamResponse.statusMessage(), url);
                        closeResponseUpstreamError(response);
                        return;
                    }
                    if (!contentType.startsWith("image/")) {
                        // todo: handle error, retry with different source
                        log.warn("Content type should be an image subtype but got '{}' for '{}'.", contentType, url);
                        closeResponseUpstreamError(response);
                        return;
                    }

                    upstreamResponse.body().onSuccess(file -> {
                        if (tileCache != null) {
                            tileCache.store(name, z, x, y, file, contentType.split("/", 2)[1]);
                        }
                        response.setStatusCode(upstreamResponse.statusCode());
                        response.putHeader("Content-Type", contentType);
                        response.putHeader("Content-Length", upstreamResponse.getHeader("Content-Length"));
                        response.end(file);
                    }).onFailure(t -> closeResponseUpstreamError(response));
                })
                .onFailure(t -> {
                    log.error("Error forwarding request to upstream server.", t);
                    closeResponseUpstreamError(response);
                });
    }

    private void closeResponseUpstreamError(HttpServerResponse response) {
        response.setStatusCode(500).end("Error requesting resource from upstream server.");
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
