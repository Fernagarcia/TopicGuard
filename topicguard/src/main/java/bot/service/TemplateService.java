package bot.service;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.HashMap;
import java.util.Map;

public class TemplateService {

    public record ValidationResult(boolean valido,
                                   Map<String, String> valores,
                                   String template,
                                   String example) {}

    public ValidationResult validarMensaje(TextChannel channel, String mensaje) {

        String topic = channel.getTopic();

        if (topic == null) {
            return null;
        }

        String template = extraerValor(topic, "TEMPLATE:");
        String example = extraerValor(topic, "EXAMPLE:");

        if (template == null) {
            return null;
        }

        String comandoEsperado = template.split(" ")[0]; // !final

        if (!mensaje.startsWith(comandoEsperado + " ")) {
            return new ValidationResult(
                    false,
                    null,
                    template,
                    example
            );
        }

        // Extraemos slug
        String[] partes = mensaje.split(" ", 2);
        if (partes.length < 2 || partes[1].isBlank()) {
            return new ValidationResult(
                    false,
                    null,
                    template,
                    example
            );
        }

        String slug = partes[1].trim();

        Map<String, String> valores = new HashMap<>();
        valores.put("slug", slug);

        return new ValidationResult(
                true,
                valores,
                template,
                example
        );
    }

    private String extraerValor(String topic, String clave) {

        for (String linea : topic.split("\n")) {
            String limpia = linea.trim();
            if (limpia.toUpperCase().startsWith(clave)) {
                return limpia.substring(clave.length()).trim();
            }
        }

        return null;
    }
}