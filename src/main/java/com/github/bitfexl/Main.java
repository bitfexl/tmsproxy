package com.github.bitfexl;

import com.github.bitfexl.tms.TMSRepository;
import com.github.bitfexl.tmsproxy.config.Config;
import com.google.gson.Gson;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        final Config config = new Gson().fromJson(new FileReader("tmsconfig.json"), Config.class);
        final int port = Objects.requireNonNullElse(config.port(), 80);
        final TMSRepository tmsRepository = new TMSRepository(config);

        final Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new BlockingHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        final String[] urlParts = exchange.getRequestURI().split("/");

                        if (urlParts.length < 5) {
                            exchange.setStatusCode(400).getResponseSender().send("Invalid number of arguments: /<tileset>/<z>/<x>/<y>");
                            return;
                        }

                        // TODO: parse arguments asynchronously

                        String name;
                        int x, y, z;

                        try {
                            name = urlParts[1];
                            z = Integer.parseInt(urlParts[2]);
                            x = Integer.parseInt(urlParts[3]);
                            y = Integer.parseInt(urlParts[4].split("\\.", 2)[0]); // allow e.g. ".png"
                        } catch (NumberFormatException ex) {
                            exchange.setStatusCode(400).getResponseSender().send("Invalid argument type (z, x, y must be numbers): /<tileset>/<z>/<x>/<y>");
                            return;
                        }

                        try {
                            final TMSRepository.TileResult result = tmsRepository.getTile(name, z, x, y);

                            if (result == null) {
                                exchange.setStatusCode(404).getResponseSender().close();
                                return;
                            }

                            if (!result.hasContent()) {
                                exchange.setStatusCode(204).getResponseSender().close();
                                return;
                            }

                            if (result.mediaType() != null) {
                                exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, result.mediaType());
                            }
                            if (result.isSizeKnown()) {
                                exchange.setResponseContentLength(result.size());
                            }

                            if (result.fileContents() != null) {
                                try (final OutputStream out = exchange.getOutputStream(); final InputStream in = result.fileContents()) {
                                    in.transferTo(out);
                                    return;
                                }
                            }

                            // TODO: optimize file response
                            if (result.filePath() != null) {
                                try (final OutputStream out = exchange.getOutputStream(); final InputStream in = new FileInputStream(result.filePath())) {
                                    in.transferTo(out);
                                    return;
                                }
                            }

                            exchange.endExchange();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            exchange.setStatusCode(500).getResponseSender().send("Internal Server Error");
                        }
                    }
                }))
                .build();

        server.start();
    }
}