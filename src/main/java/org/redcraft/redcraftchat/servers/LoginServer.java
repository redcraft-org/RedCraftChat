package org.redcraft.redcraftchat.servers;

/**
 * What a player's "send me here when I log in" preference means.
 *
 * Three answers share one column, which is why the sentinel is spelled with a
 * character a server id cannot contain: a network is free to register a
 * backend called "last", and that must not silently become "wherever I was".
 *
 * <ul>
 * <li>null or empty: leave it to the proxy, which is the try list, which is
 * the hub. This is the default and costs nothing to store for the players who
 * never touch the setting.
 * <li>{@link #LAST}: whichever server they were on when they left.
 * <li>anything else: that server id, every time, no matter where they were.
 * </ul>
 */
public final class LoginServer {

    /** Not a legal server id, so it can never collide with one. */
    public static final String LAST = "@last";

    private LoginServer() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * The server to put them on, or null to leave the choice to the proxy.
     *
     * Pure: the two stored values in, a server id or nothing out. Whether that
     * id still exists is the caller's problem, because a preference outlives
     * the server it names.
     */
    public static String resolve(String loginServer, String lastServer) {
        if (loginServer == null || loginServer.isEmpty()) {
            return null;
        }
        if (LAST.equals(loginServer)) {
            // A player who picked "last server" before ever moving has nothing
            // recorded yet, and the proxy's own answer is the right one
            return lastServer == null || lastServer.isEmpty() ? null : lastServer;
        }
        return loginServer;
    }

    /**
     * The server a player with no preference actually lands on: the first
     * entry of the proxy's try list that the proxy still has.
     *
     * Read rather than assumed. It is the hub today, but "the hub" is a fact
     * about one config file, and a setting screen that says so out loud must
     * not be the thing that goes stale when that file changes.
     */
    public static String effectiveDefault(java.util.List<String> attemptOrder,
            java.util.Collection<String> known) {
        if (attemptOrder == null) {
            return null;
        }
        for (String candidate : attemptOrder) {
            if (candidate != null && !candidate.isEmpty()
                    && (known == null || known.contains(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /** Whether the setting names one fixed server rather than a rule. */
    public static boolean isFixedServer(String loginServer) {
        return loginServer != null && !loginServer.isEmpty() && !LAST.equals(loginServer);
    }
}
