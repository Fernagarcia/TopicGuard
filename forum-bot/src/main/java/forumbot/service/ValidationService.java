package forumbot.service;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.concurrent.TimeUnit;

public class ValidationService {

    private static final int MAX_PALABRAS_TITULO = 6;

    public boolean tituloValido(String titulo) {
        String[] palabras = titulo.trim().split("\\s+");
        return palabras.length <= MAX_PALABRAS_TITULO;
    }

    public void rechazarTituloPorLongitud(ThreadChannel thread) {
        thread.sendMessage("""
                ❌ **El título de tu publicación es demasiado largo.**

                El título debe tener un máximo de **%d palabras**, corto y descriptivo.

                > ✅ `Error conexión base de datos`
                > ❌ `tengo un problema con java y no sé qué hacer`

                Este foro se cerrará en 60 segundos. Volvé a crear tu publicación con un título más corto.
                """.formatted(MAX_PALABRAS_TITULO))
                .queue(
                        msg -> thread.getManager()
                                .setArchived(true)
                                .setLocked(true)
                                .queueAfter(60, TimeUnit.SECONDS),
                        error -> System.err.println("[ValidationService] No se pudo enviar aviso de título largo: "
                                + error.getMessage())
                );
    }
}