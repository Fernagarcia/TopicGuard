package forumbot.service;

import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.List;
import java.util.Optional;

public class TagService {

    private final ServerSettingsService settingsService;

    public TagService(ServerSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void aplicarTagInicial(ThreadChannel thread) {
        long serverId = thread.getGuild().getIdLong();

        settingsService.getDefaultTagId(serverId).ifPresent(tagId -> {

            if (!(thread.getParentChannel() instanceof ForumChannel forum)) return;

            Optional<ForumTag> tag = forum.getAvailableTags().stream()
                    .filter(t -> t.getIdLong() == tagId)
                    .findFirst();

            if (tag.isEmpty()) {
                System.err.println("[TagService] Tag con id " + tagId + " no encontrado en el foro");
                return;
            }

            thread.getManager()
                    .setAppliedTags(List.of(tag.get()))
                    .queue(
                            null,
                            error -> System.err.println("[TagService] No se pudo aplicar tag: " + error.getMessage())
                    );
        });
    }
}