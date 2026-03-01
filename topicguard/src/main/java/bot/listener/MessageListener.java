package bot.listener;

import bot.service.DecisionService;
import bot.service.ThreadService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;


public class MessageListener extends ListenerAdapter {

    private final ThreadService threadService;
    private final DecisionService decisionService;

    public MessageListener(ThreadService threadService,
                           DecisionService decisionService) {
        this.threadService = threadService;
        this.decisionService = decisionService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        threadService.handleMessage(event);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        decisionService.handleReaction(event);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if (!event.getName().equals("stats")) return;

        threadService.handleStatsCommand(event);
    }
}