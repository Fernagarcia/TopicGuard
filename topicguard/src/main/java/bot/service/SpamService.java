package bot.service;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpamService {

    private final Map<Long, Long> lastCreation = new ConcurrentHashMap<>();
    private final ServerSettingsService settingsService;

    public SpamService(ServerSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public boolean permitido(MessageReceivedEvent event) {

        long userId = event.getAuthor().getIdLong();
        long serverId = event.getGuild().getIdLong();
        long now = System.currentTimeMillis();
        long cooldown = settingsService.getCooldown(serverId);

        System.out.println("[SpamService] userId=" + userId + " serverId=" + serverId + " cooldown=" + cooldown + "ms");

        Long last = lastCreation.get(userId);

        if (last != null && now - last < cooldown) {
            System.out.println("[SpamService] Bloqueado, tiempo restante: " + (cooldown - (now - last)) + "ms");
            return false;
        }

        lastCreation.put(userId, now);
        return true;
    }

    public boolean permitidoEnForo(ThreadChannel thread) {

        long userId = thread.getOwnerIdLong(); // quien creó el post
        long serverId = thread.getGuild().getIdLong();
        long now = System.currentTimeMillis();
        long cooldown = settingsService.getCooldown(serverId);

        Long last = lastCreation.get(userId);

        if (last != null && now - last < cooldown) {
            return false;
        }

        lastCreation.put(userId, now);
        return true;
    }
}