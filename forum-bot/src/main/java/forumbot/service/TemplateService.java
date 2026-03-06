package forumbot.service;

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

        // Eliminamos el comando
        String resto = mensaje.substring(comandoEsperado.length()).trim();

        if (resto.isBlank()) {
            return new ValidationResult(
                    false,
                    null,
                    template,
                    example
            );
        }

        // 🔹 Extraemos solo hasta el primer espacio
        String[] partes = resto.split("\\s+", 2);

        String slug = partes[0];

        // Validación básica del slug
        if (!slug.matches("[a-zA-Z0-9-]{3,}")) {
            return new ValidationResult(
                    false,
                    null,
                    template,
                    example
            );
        }

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