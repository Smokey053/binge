package com.binge.player;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Serves the small HTML page that hosts the YouTube IFrame player from {http://127.0.0.1}.
YouTube won't play embedded videos from {file} or {about} origins, so the page needs a real HTTP origin. 
Only this one page is served, and only to the local machine. */
final class PlayerServer implements AutoCloseable {

    private static final String PAGE_RESOURCE = "/com/binge/player.html";

    private HttpServer server;
    private ExecutorService executor;

    /** Starts the server (once) and returns the page URL. */
    synchronized String start() {
        if (server == null) {
            try {
                byte[] page = readPage();
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
                executor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "binge-player-server");
                    t.setDaemon(true);
                    return t;
                });
                server.setExecutor(executor);
                server.createContext("/player", exchange -> {
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    exchange.getResponseHeaders().add("Cache-Control", "no-store");
                    exchange.sendResponseHeaders(200, page.length);
                    try (var body = exchange.getResponseBody()) {
                        body.write(page);
                    }
                });
                server.start();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not start the local player page", e);
            }
        }
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/player";
    }

    private static byte[] readPage() throws IOException {
        try (InputStream in = PlayerServer.class.getResourceAsStream(PAGE_RESOURCE)) {
            if (in == null) {
                throw new IOException("Missing resource " + PAGE_RESOURCE);
            }
            return in.readAllBytes();
        }
    }

    @Override
    public synchronized void close() {
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
            server = null;
        }
    }
}
