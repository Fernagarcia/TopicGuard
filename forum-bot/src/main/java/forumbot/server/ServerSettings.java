package forumbot.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ServerSettings {

    private final long serverId;
    private long threadCooldownMs;
    private Long logChannelId;
    private Long defaultTagId;
    private Set<Long> allowedRoleIds; // roles que pueden usar comandos de moderación

    public ServerSettings(long serverId) {
        this.serverId = serverId;
        this.threadCooldownMs = Defaults.COOLDOWN_MS;
        this.logChannelId = null;
        this.defaultTagId = null;
        this.allowedRoleIds = new HashSet<>();
    }

    public long getServerId() { return serverId; }

    public long getThreadCooldownMs() { return threadCooldownMs; }
    public void setThreadCooldownMs(long cooldownMs) { this.threadCooldownMs = cooldownMs; }

    public Optional<Long> getLogChannelId() { return Optional.ofNullable(logChannelId); }
    public void setLogChannelId(Long logChannelId) { this.logChannelId = logChannelId; }

    public Optional<Long> getDefaultTagId() { return Optional.ofNullable(defaultTagId); }
    public void setDefaultTagId(Long defaultTagId) { this.defaultTagId = defaultTagId; }

    public Set<Long> getAllowedRoleIds() { return Collections.unmodifiableSet(allowedRoleIds); }
    public void addAllowedRole(long roleId) { this.allowedRoleIds.add(roleId); }
    public void removeAllowedRole(long roleId) { this.allowedRoleIds.remove(roleId); }

    public static final class Defaults {
        public static final long COOLDOWN_MS = 0;
        private Defaults() {}
    }
}