package bot.listener;

import bot.service.DecisionService;
import bot.orchestrator.MessageOrchestrator;
import bot.orchestrator.ForumPostOrchestrator;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

    private final MessageOrchestrator orchestrator;
    private final DecisionService decisionService;
    private final ForumPostOrchestrator forumPostOrchestrator;

    public MessageListener(MessageOrchestrator orchestrator,
                           DecisionService decisionService,
                           ForumPostOrchestrator forumPostOrchestrator) {

        this.orchestrator = orchestrator;
        this.decisionService = decisionService;
        this.forumPostOrchestrator = forumPostOrchestrator;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        orchestrator.process(event);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        decisionService.handleReaction(event);
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!(event.getChannel() instanceof ThreadChannel thread)) return;

        if (thread.getParentChannel() instanceof ForumChannel) {
            // Hilo creado desde un foro → ForumPostOrchestrator
            forumPostOrchestrator.processNewPost(thread);

        }
    }
}