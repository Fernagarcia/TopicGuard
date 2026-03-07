package forumbot.service;

import forumbot.repository.ServerSettingsRepository;
import forumbot.server.ServerSettings;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSettingsService {

    private final Map<Long, ServerSettings> settings;
    private final ServerSettingsRepository repository;

    public ServerSettingsService(ServerSettingsRepository repository) {
        this.repository = repository;
        this.settings = new ConcurrentHashMap<>(repository.loadAll());
    }

    public ServerSettings getOrCreate(long serverId) {
        return settings.computeIfAbsent(serverId, ServerSettings::new);
    }

    public long getCooldown(long serverId) {
        return getOrCreate(serverId).getThreadCooldownMs();
    }

    public void setCooldown(long serverId, long cooldownMs) {
        getOrCreate(serverId).setThreadCooldownMs(cooldownMs);
        repository.saveAll(settings); // persiste inmediatamente
    }

    public void setLogChannel(long serverId, long channelId) {
        getOrCreate(serverId).setLogChannelId(channelId);
        repository.saveAll(settings);
    }

    public Optional<Long> getLogChannelId(long serverId) {
        return getOrCreate(serverId).getLogChannelId();
    }

    public void setDefaultTag(long serverId, long tagId) {
        getOrCreate(serverId).setDefaultTagId(tagId);
        repository.saveAll(settings);
    }

    public Optional<Long> getDefaultTagId(long serverId) {
        return getOrCreate(serverId).getDefaultTagId();
    }
}