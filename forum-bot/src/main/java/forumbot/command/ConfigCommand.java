package forumbot.command;

import forumbot.service.ServerSettingsService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.Set;
import java.util.stream.Collectors;

public class ConfigCommand extends ListenerAdapter {

    private final ServerSettingsService settingsService;

    public ConfigCommand(ServerSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("config")) return;

        String subcommand = event.getSubcommandName();
        String group = event.getSubcommandGroup();

        // Para allowedrole, la clave es el grupo
        String commandKey = group != null ? group : subcommand;

        boolean isAdminOnly = commandKey.equals("allowedrole");
        boolean hasPermission = isAdminOnly
                ? event.getMember().hasPermission(Permission.ADMINISTRATOR)
                : settingsService.canModerate(event.getMember(), event.getGuild().getIdLong());

        if (!hasPermission) {
            event.reply("❌ No tenés permisos para usar este comando.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        switch (commandKey) {
            case "cooldown"     -> handleCooldown(event);
            case "logchannel"   -> handleLogChannel(event);
            case "defaulttag"   -> handleDefaultTag(event);
            case "allowedrole"  -> handleAllowedRole(event);
        }
    }

    private void handleAllowedRole(SlashCommandInteractionEvent event) {
        String group = event.getSubcommandName(); // "add", "remove", "list"
        long serverId = event.getGuild().getIdLong();

        switch (group != null ? group : "") {
            case "add" -> {
                OptionMapping option = event.getOption("rol");
                if (option == null) return;
                long roleId = option.getAsRole().getIdLong();
                String roleName = option.getAsRole().getName();
                settingsService.addAllowedRole(serverId, roleId);
                event.reply("✅ Rol **%s** agregado como moderador del bot.".formatted(roleName))
                        .setEphemeral(true).queue();
            }
            case "remove" -> {
                OptionMapping option = event.getOption("rol");
                if (option == null) return;
                long roleId = option.getAsRole().getIdLong();
                String roleName = option.getAsRole().getName();
                settingsService.removeAllowedRole(serverId, roleId);
                event.reply("✅ Rol **%s** removido.".formatted(roleName))
                        .setEphemeral(true).queue();
            }
            case "list" -> {
                Set<Long> roles = settingsService.getAllowedRoleIds(serverId);
                if (roles.isEmpty()) {
                    event.reply("📋 No hay roles configurados. Solo los administradores pueden usar el bot.")
                            .setEphemeral(true).queue();
                } else {
                    String lista = roles.stream()
                            .map(id -> "<@&%d>".formatted(id))
                            .collect(Collectors.joining("\n"));
                    event.reply("📋 **Roles con permisos de moderación:**\n" + lista)
                            .setEphemeral(true).queue();
                }
            }
            default -> event.reply("❌ Acción no reconocida.").setEphemeral(true).queue();
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