package bot.service;

import bot.repository.ServerSettingsRepository;
import bot.server.ServerSettings;

import java.util.Map;
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
}