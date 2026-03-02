package bot.service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpamService {

    private final Map<Long, Long> lastCreation = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 360_000; // 5 minutos

    public boolean permitido(MessageReceivedEvent event) {

        long userId = event.getAuthor().getIdLong();
        long now = System.currentTimeMillis();

        Long last = lastCreation.get(userId);

        if (last != null && now - last < COOLDOWN_MS) {
            return false;
        }

        lastCreation.put(userId, now);
        return true;
    }
}