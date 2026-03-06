package forumbot.service.similarity;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

public record SimilarityResult(
        ThreadChannel thread,
        double score,
        MatchType type
) {}