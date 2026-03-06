package bot.repository;

import bot.server.ServerSettings;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerSettingsRepository {

    private static final String FILE_PATH = "src/main/java/bot/data/server_settings.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<Long, ServerSettings> loadAll() {
        File file = new File(FILE_PATH);

        if (!file.exists()) return new HashMap<>();

        try {
            ServerSettingsData[] data = mapper.readValue(file, ServerSettingsData[].class);
            Map<Long, ServerSettings> result = new HashMap<>();

            for (ServerSettingsData d : data) {
                ServerSettings s = new ServerSettings(d.serverId);
                s.setThreadCooldownMs(d.cooldownMs);
                s.setLogChannelId(d.logChannelId); // nuevo
                result.put(d.serverId, s);
            }

            return result;

        } catch (IOException e) {
            System.err.println("Error leyendo configuración: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public void saveAll(Map<Long, ServerSettings> settings) {
        try {
            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();

            ServerSettingsData[] data = settings.values().stream()
                    .map(s -> new ServerSettingsData(
                            s.getServerId(),
                            s.getThreadCooldownMs(),
                            s.getLogChannelId().orElse(null) // nuevo
                    ))
                    .toArray(ServerSettingsData[]::new);

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);

        } catch (IOException e) {
            System.err.println("Error guardando configuración: " + e.getMessage());
        }
    }

    public static class ServerSettingsData {
        public long serverId;
        public long cooldownMs;
        public Long logChannelId;

        public ServerSettingsData() {}

        public ServerSettingsData(long serverId, long cooldownMs, Long logChannelId) {
            this.serverId = serverId;
            this.cooldownMs = cooldownMs;
            this.logChannelId = logChannelId;
        }
    }
}