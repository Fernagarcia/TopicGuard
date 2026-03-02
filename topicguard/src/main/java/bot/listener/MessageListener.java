package bot.listener;

import bot.service.DecisionService;
import bot.orchestrator.MessageOrchestrator;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

    private final MessageOrchestrator orchestrator;
    private final DecisionService decisionService;

    public MessageListener(MessageOrchestrator orchestrator,
                           DecisionService decisionService) {
        this.orchestrator = orchestrator;
        this.decisionService = decisionService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        orchestrator.process(event);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        decisionService.handleReaction(event);
    }
}