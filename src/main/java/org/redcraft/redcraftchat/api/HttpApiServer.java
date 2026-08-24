package org.redcraft.redcraftchat.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Serves the player and version lists the website reads.
 *
 * This used to be its own BungeeCord plugin. It is small, it only reports what
 * the proxy already knows, and now that it also reports the languages a player
 * speaks it belongs next to the code that owns them.
 *
 * :warning: The server binds a plain HTTP port with no authentication, so bind
 * it to an address only your own services can reach.
 */
public class HttpApiServer {

    private HttpServer server;

    public void start() {
        if (!Config.jsonApiEnabled) {
            return;
        }

        try {
            InetAddress address = InetAddress.getByName(Config.jsonApiBind);
            InetSocketAddress socketAddress = new InetSocketAddress(address, Config.jsonApiPort);

            server = HttpServer.create(socketAddress, 0);
            server.createContext("/players.json", exchange -> respondJson(exchange, PlayerList.build().getOnlinePlayersJson()));
            server.createContext("/versions.json", exchange -> respondJson(exchange, VersionInfo.build().getVersionsJson()));
            server.setExecutor(null);
            server.start();

            RedCraftChat.getInstance().getLogger().info(
                    "Json api listening on " + Config.jsonApiBind + ":" + Config.jsonApiPort);
        } catch (IOException e) {
            RedCraftChat.getInstance().getLogger().error("Could not start the json api", e);
        }
    }

    public void stop() {
        if (server != null) {
            // Gracefully stop the server and give 1 second to handle current requests
            server.stop(1);
        }
    }

    static void respondJson(HttpExchange exchange, String json) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Cache-Control", "no-cache");

        // Measured in bytes, not characters, a display name with a colour code
        // or an accent is longer than it looks and used to truncate the body
        byte[] body = json.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
