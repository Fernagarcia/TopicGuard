package bot.service;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateService {

    public TemplateMatch validarMensaje(TextChannel channel, String mensaje) {

        String topic = channel.getTopic();

        if (topic == null || !topic.startsWith("TEMPLATE:"))
            return null;

        if (mensaje == null || mensaje.isBlank())
            return TemplateMatch.invalid();

        // Extraer template
        String plantilla = topic.replaceFirst("TEMPLATE:", "").trim();

        /*
         * Esperamos formato tipo:
         * !comando {slug}
         */

        Pattern templatePattern = Pattern.compile("!(\\w+)\\s+\\{([^}]+)}");
        Matcher templateMatcher = templatePattern.matcher(plantilla);

        if (!templateMatcher.find())
            return TemplateMatch.invalid();

        String comando = templateMatcher.group(1);

        // Construimos regex dinámica basada en el comando
        Pattern mensajePattern = Pattern.compile(
                "^!" + Pattern.quote(comando) + "\\s+([a-zA-Z0-9-]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher mensajeMatcher = mensajePattern.matcher(mensaje.trim());

        if (!mensajeMatcher.find())
            return TemplateMatch.invalid();

        String slug = mensajeMatcher.group(1);

        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("slug", slug);

        return TemplateMatch.valid(valores);
    }

    public record TemplateMatch(boolean valido,
                                Map<String, String> valores) {

        public static TemplateMatch valid(Map<String, String> valores) {
            return new TemplateMatch(true, valores);
        }

        public static TemplateMatch invalid() {
            return new TemplateMatch(false, null);
        }
    }
}