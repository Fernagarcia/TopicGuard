package forumbot.command;

import forumbot.service.ServerSettingsService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class ConfigCommand extends ListenerAdapter {

    private final ServerSettingsService settingsService;

    public ConfigCommand(ServerSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("config")) return;

        // Por ahora solo admins, en el futuro se puede verificar rol específico aquí
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ No tenés permisos para usar este comando.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) return;

        switch (subcommand) {
            case "cooldown" -> handleCooldown(event);
            case "logchannel" -> handleLogChannel(event);
            case "defaulttag"  -> handleDefaultTag(event);
            // Futuros subcomandos se agregan aquí
        }
    }

    private void handleDefaultTag(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("tag");
        if (option == null) return;

        // El admin escribe el nombre del tag y el bot busca su ID
        String tagName = option.getAsString();
        long serverId = event.getGuild().getIdLong();

        // Buscamos el tag en todos los foros del servidor
        event.getGuild().getForumChannels().stream()
                .flatMap(forum -> forum.getAvailableTags().stream())
                .filter(tag -> tag.getName().equalsIgnoreCase(tagName))
                .findFirst()
                .ifPresentOrElse(
                        tag -> {
                            try {
                                settingsService.setDefaultTag(serverId, tag.getIdLong());
                                event.reply("✅ Tag inicial configurado: **" + tag.getName() + "**")
                                        .setEphemeral(true)
                                        .queue();
                            } catch (RuntimeException e) {
                                event.reply("❌ No se pudo guardar la configuración, intentá de nuevo.")
                                        .setEphemeral(true)
                                        .queue();
                            }
                        },
                        () -> event.reply("❌ No se encontró ningún tag con ese nombre en los foros del servidor.")
                                .setEphemeral(true)
                                .queue()
                );
    }

    private void handleCooldown(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("segundos");
        if (option == null) return;

        long segundos = option.getAsLong();

        if (segundos < 0) {
            event.reply("❌ El cooldown no puede ser negativo.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long serverId = event.getGuild().getIdLong();
        settingsService.setCooldown(serverId, segundos * 1000);

        event.reply("✅ Cooldown actualizado a **%d segundos**.".formatted(segundos))
                .setEphemeral(true)
                .queue();
    }

    private void handleLogChannel(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("canal");
        if (option == null) return;

        long channelId = option.getAsChannel().getIdLong();
        long serverId = event.getGuild().getIdLong();

        settingsService.setLogChannel(serverId, channelId);

        event.reply("✅ Canal de logs configurado: <#%d>".formatted(channelId))
                .setEphemeral(true)
                .queue();
    }
}