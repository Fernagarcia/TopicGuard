package forumbot.repository;

import forumbot.server.ServerSettings;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServerSettingsRepository {

    private static final String FILE_PATH = "data/server_settings.json";
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
                s.setLogChannelId(d.logChannelId);
                s.setDefaultTagId(d.defaultTagId); // nuevo
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

            System.out.println("[Repository] Guardando en: " + file.getAbsolutePath());

            ServerSettingsData[] data = settings.values().stream()
                    .map(s -> new ServerSettingsData(
                            s.getServerId(),
                            s.getThreadCooldownMs(),
                            s.getLogChannelId().orElse(null),
                            s.getDefaultTagId().orElse(null) // nuevo
                    ))
                    .toArray(ServerSettingsData[]::new);

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, data);
            System.out.println("[Repository] Guardado correctamente");

        } catch (IOException e) {
            System.err.println("[Repository] Error crítico guardando configuración: " + e.getMessage());
            throw new RuntimeException("No se pudo persistir la configuración", e);
        }
    }
    public static class ServerSettingsData {
        public long serverId;
        public long cooldownMs;
        public Long logChannelId;
        public Long defaultTagId; // nuevo

        public ServerSettingsData() {}

        public ServerSettingsData(long serverId, long cooldownMs,
                                  Long logChannelId, Long defaultTagId) {
            this.serverId = serverId;
            this.cooldownMs = cooldownMs;
            this.logChannelId = logChannelId;
            this.defaultTagId = defaultTagId;
        }
    }
}