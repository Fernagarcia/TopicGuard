package bot.server;

import java.util.Optional;

public class ServerSettings {

    private final long serverId;
    private long threadCooldownMs;
    private Long logChannelId; // null = no configurado

    public ServerSettings(long serverId) {
        this.serverId = serverId;
        this.threadCooldownMs = Defaults.COOLDOWN_MS;
        this.logChannelId = null;
    }

    public long getServerId() { return serverId; }

    public long getThreadCooldownMs() { return threadCooldownMs; }
    public void setThreadCooldownMs(long cooldownMs) { this.threadCooldownMs = cooldownMs; }

    public Optional<Long> getLogChannelId() { return Optional.ofNullable(logChannelId); }
    public void setLogChannelId(Long logChannelId) { this.logChannelId = logChannelId; }

    public static final class Defaults {
        public static final long COOLDOWN_MS = 0;
        private Defaults() {}
    }
}