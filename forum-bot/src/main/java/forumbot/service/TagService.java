package forumbot.service;

import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

            List<ForumTag> tagsActuales = thread.getAppliedTags();
            boolean yaTieneElTag = tagsActuales.stream()
                    .anyMatch(t -> t.getIdLong() == tagId);

            if (yaTieneElTag) return;

            List<ForumTag> tagsMergeados = new ArrayList<>(tagsActuales);
            tagsMergeados.add(tag.get());

            thread.getManager()
                    .setAppliedTags(tagsMergeados)
                    .queueAfter(
                            1, TimeUnit.SECONDS,
                            null,
                            error -> System.err.println("[TagService] No se pudo aplicar tag: " + error.getMessage())
                    );
        });
    }
}