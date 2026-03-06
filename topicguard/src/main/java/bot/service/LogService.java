package bot.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import java.awt.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JDA jda;
    private final ServerSettingsService settingsService;

    public LogService(JDA jda, ServerSettingsService settingsService) {
        this.jda = jda;
        this.settingsService = settingsService;
    }

    public void logForumCreated(ThreadChannel thread) {
        send(thread, new EmbedBuilder()
                .setTitle("🟢 Nuevo foro creado")
                .setColor(Color.GREEN)
                .addField("Título", thread.getName(), false)
                .addField("Autor", "<@" + thread.getOwnerIdLong() + ">", true)
                .addField("Link", "[Ver foro](" + thread.getJumpUrl() + ")", true)
                .setFooter(now())
                .build()
        );
    }

    public void logForumClosedExact(ThreadChannel thread) {
        send(thread, new EmbedBuilder()
                .setTitle("🔴 Foro cerrado — duplicado exacto")
                .setColor(Color.RED)
                .addField("Título", thread.getName(), false)
                .addField("Autor", "<@" + thread.getOwnerIdLong() + ">", true)
                .setFooter(now())
                .build()
        );
    }

    public void logForumClosedByUser(ThreadChannel thread) {
        send(thread, new EmbedBuilder()
                .setTitle("🟡 Foro cerrado — autor confirmó duplicado")
                .setColor(Color.YELLOW)
                .addField("Título", thread.getName(), false)
                .addField("Autor", "<@" + thread.getOwnerIdLong() + ">", true)
                .setFooter(now())
                .build()
        );
    }

    public void logForumClosedByTimeout(ThreadChannel thread) {
        send(thread, new EmbedBuilder()
                .setTitle("⏰ Foro cerrado — sin respuesta del autor")
                .setColor(Color.ORANGE)
                .addField("Título", thread.getName(), false)
                .addField("Autor", "<@" + thread.getOwnerIdLong() + ">", true)
                .setFooter(now())
                .build()
        );
    }

    private void send(ThreadChannel thread, MessageEmbed embed) {
        long serverId = thread.getGuild().getIdLong();

        settingsService.getLogChannelId(serverId).ifPresent(channelId -> {
            TextChannel logChannel = jda.getTextChannelById(channelId);
            if (logChannel == null) return;
            logChannel.sendMessageEmbeds(embed).queue();
        });
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}