package forumbot.orchestrator;

import forumbot.service.FeedbackService;
import forumbot.service.SpamService;
import forumbot.service.TemplateService;
import forumbot.service.ThreadService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MessageOrchestrator {

    private final TemplateService templateService;
    private final SpamService spamService;
    private final FeedbackService feedbackService;
    private final ThreadService threadService;

    public MessageOrchestrator(TemplateService templateService,
                               SpamService spamService,
                               FeedbackService feedbackService,
                               ThreadService threadService) {

        this.templateService = templateService;
        this.spamService = spamService;
        this.feedbackService = feedbackService;
        this.threadService = threadService;
    }

    public void process(MessageReceivedEvent event) {

        if (event.getAuthor().isBot()) return;
        if (!(event.getChannel() instanceof TextChannel channel)) return;

        String mensaje = event.getMessage().getContentRaw();

        var validation = templateService.validarMensaje(channel, mensaje);

        if (validation == null) return; // canal sin template

        if (!validation.valido()) {
            feedbackService.mostrarTemplateIncorrecto(
                    event,
                    validation.template(),
                    validation.example()
            );
            return;
        }

        if (!spamService.permitido(event)) {
            feedbackService.mostrarCooldown(event);
            return;
        }

        String slug = validation.valores().get("slug");

        String contenido = mensaje
                .replaceFirst("^!\\w+\\s+" + slug, "")
                .trim();

        threadService.handleValidMessage(
                event,
                channel,
                slug,
                contenido
        );
    }
}