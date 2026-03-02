package bot.service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.concurrent.TimeUnit;

public class FeedbackService {

    public void mostrarTemplateIncorrecto(MessageReceivedEvent event,
                                          String template,
                                          String example) {

        String mensaje = """
                📌 Formato esperado:

                %s

                Ejemplo:
                %s
                """.formatted(
                template,
                example != null ? example : "No definido"
        );

        event.getChannel()
                .sendMessage(event.getAuthor().getAsMention() + " " + mensaje)
                .queue(msg ->
                        msg.delete().queueAfter(10, TimeUnit.SECONDS)
                );

        event.getMessage().delete().queue();
    }

    public void mostrarCooldown(MessageReceivedEvent event) {

        event.getChannel()
                .sendMessage(event.getAuthor().getAsMention() +
                        " Debes esperar 5 minutos antes de crear otro hilo.")
                .queue(msg ->
                        msg.delete().queueAfter(5, TimeUnit.SECONDS)
                );

        event.getMessage().delete().queue();
    }
}