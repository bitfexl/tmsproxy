package com.github.bitfexl.tmsproxy.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultHandler implements Handler<RoutingContext> {
    public DefaultHandler(Router router) {
        router.route().handler(this);
    }

    @Override
    public void handle(RoutingContext ctx) {
        log.info("{} {} not found", ctx.request().method(), ctx.request().path());
        ctx.response().setStatusCode(404).end();
    }
}
