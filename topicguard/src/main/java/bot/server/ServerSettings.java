package bot.server;

public class ServerSettings {

    private final long serverId;
    private long threadCooldownMs;

    // Futuras configuraciones se agregan aquí
    // private boolean autoCloseEnabled;
    // private int maxThreadsPerDay;

    public ServerSettings(long serverId) {
        this.serverId = serverId;
        this.threadCooldownMs = Defaults.COOLDOWN_MS;
    }

    public long getServerId() { return serverId; }

    public long getThreadCooldownMs() { return threadCooldownMs; }
    public void setThreadCooldownMs(long cooldownMs) { this.threadCooldownMs = cooldownMs; }

    // Los defaults centralizados para no tener magic numbers dispersos
    public static final class Defaults {
        public static final long COOLDOWN_MS = 0;

        private Defaults() {}
    }
}